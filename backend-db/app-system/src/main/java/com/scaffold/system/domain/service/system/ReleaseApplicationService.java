package com.scaffold.system.domain.service.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.config.ReleaseProperties;
import com.scaffold.system.domain.model.system.ReleaseVersionMeta;
import com.scaffold.system.domain.vo.system.ReleaseActivateResultVO;
import com.scaffold.system.domain.vo.system.ReleaseCurrentVO;
import com.scaffold.system.domain.vo.system.ReleaseInfoVO;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 内网 zip 在线更版：上传校验、版本列表、软链激活/回滚、延迟重启。
 * <p>
 * 状态以文件系统为准（{@code current}/{@code previous} 软链 + {@code releases/}），不落库。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-08-12
 */
@Slf4j
@Service
public class ReleaseApplicationService {

    public static final String APP_JAR = "app.jar";
    public static final String VERSION_JSON = "version.json";
    public static final String CURRENT_LINK = "current";
    public static final String PREVIOUS_LINK = "previous";
    public static final String RELEASES_DIR = "releases";
    public static final String INCOMING_DIR = "incoming";

    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
    private static final Set<String> ALLOWED_ZIP_ENTRIES = Set.of(APP_JAR, VERSION_JSON);

    private final ReleaseProperties properties;
    private final ObjectMapper objectMapper;
    private final Runnable restartAction;
    private final ScheduledExecutorService restartScheduler;
    private final AtomicBoolean restartScheduled = new AtomicBoolean(false);
    private final Object activateLock = new Object();

    /**
     * 注入构造器；因存在测试专用重载，必须显式标注 @Autowired，否则 Spring 会回退无参构造。
     */
    @Autowired
    public ReleaseApplicationService(ReleaseProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, () -> System.exit(0), true);
    }

    private ReleaseApplicationService(
            ReleaseProperties properties, ObjectMapper objectMapper, Runnable restartAction, boolean ignored) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restartAction = restartAction;
        this.restartScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "app-release-restart");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 单测用：可注入 restartAction，避免触发 JVM 退出。
     */
    static ReleaseApplicationService createForTest(
            ReleaseProperties properties, ObjectMapper objectMapper, Runnable restartAction) {
        return new ReleaseApplicationService(properties, objectMapper, restartAction, true);
    }

    @PreDestroy
    void shutdown() {
        restartScheduler.shutdownNow();
    }

    public List<ReleaseInfoVO> listReleases() {
        Path home = requireHome();
        String current = resolveLinkVersion(home, CURRENT_LINK).orElse(null);
        String previous = resolveLinkVersion(home, PREVIOUS_LINK).orElse(null);
        Path releases = home.resolve(RELEASES_DIR);
        if (!Files.isDirectory(releases)) {
            return List.of();
        }
        List<ReleaseInfoVO> result = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(releases)) {
            dirs.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString(), this::compareVersionDesc))
                    .forEach(dir -> {
                        String version = dir.getFileName().toString();
                        if (!VERSION_PATTERN.matcher(version).matches()) {
                            return;
                        }
                        try {
                            ReleaseInfoVO vo = toInfo(dir, version, current, previous);
                            result.add(vo);
                        } catch (IOException ex) {
                            log.warn("skip unreadable release dir {}", dir, ex);
                        }
                    });
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_HOME_UNAVAILABLE, "无法读取发布目录: " + ex.getMessage());
        }
        return result;
    }

    public ReleaseCurrentVO getCurrent() {
        Path home = requireHome();
        ReleaseCurrentVO vo = new ReleaseCurrentVO();
        Optional<String> current = resolveLinkVersion(home, CURRENT_LINK);
        Optional<String> previous = resolveLinkVersion(home, PREVIOUS_LINK);
        vo.setCurrentVersion(current.orElse(null));
        vo.setPreviousVersion(previous.orElse(null));
        vo.setRunningVersion(resolveRunningVersion());
        current.ifPresent(v -> {
            Path dir = home.resolve(RELEASES_DIR).resolve(v);
            try {
                ReleaseVersionMeta meta = readMeta(dir);
                if (meta != null) {
                    vo.setBuildTime(meta.getBuildTime());
                    vo.setGitCommit(meta.getGitCommit());
                    vo.setJarSha256(meta.getJarSha256());
                }
            } catch (IOException ex) {
                log.warn("failed to read current version meta {}", dir, ex);
            }
        });
        return vo;
    }

    public ReleaseInfoVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCodeConstants.MISSING_PARAM, "请上传 zip 发布包");
        }
        if (file.getSize() > properties.getMaxUploadBytes()) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID,
                    "发布包超过大小限制: " + properties.getMaxUploadBytes() + " bytes");
        }
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("release.zip");
        if (!original.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID, "发布包必须是 .zip 文件");
        }

        Path home = requireHome();
        ensureLayout(home);
        Path incoming = home.resolve(INCOMING_DIR);
        Path tempZip = null;
        Path stagingDir = null;
        try {
            tempZip = Files.createTempFile(incoming, "upload-", ".zip");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempZip, StandardCopyOption.REPLACE_EXISTING);
            }

            stagingDir = Files.createTempDirectory(incoming, "staging-");
            extractAllowedZip(tempZip, stagingDir);

            Path jar = stagingDir.resolve(APP_JAR);
            Path metaFile = stagingDir.resolve(VERSION_JSON);
            if (!Files.isRegularFile(jar) || !Files.isRegularFile(metaFile)) {
                throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID,
                        "zip 必须包含 app.jar 与 version.json");
            }

            ReleaseVersionMeta meta = objectMapper.readValue(Files.readString(metaFile, StandardCharsets.UTF_8),
                    ReleaseVersionMeta.class);
            validateMeta(meta);

            String actualSha = sha256Hex(jar);
            if (!actualSha.equalsIgnoreCase(meta.getJarSha256().trim())) {
                throw new BusinessException(ErrorCodeConstants.RELEASE_CHECKSUM_MISMATCH,
                        "app.jar 校验和不匹配");
            }

            String version = meta.getVersion().trim();
            String currentVersion = resolveLinkVersion(home, CURRENT_LINK).orElse(null);
            if (version.equals(currentVersion)) {
                throw new BusinessException(ErrorCodeConstants.RELEASE_CURRENT_OVERWRITE_FORBIDDEN,
                        "禁止覆盖当前运行版本 " + version + "，请使用新版本号或先切换到其他版本");
            }

            Path targetDir = home.resolve(RELEASES_DIR).resolve(version);
            // 先装到 sibling 临时目录再原子替换，避免半写入
            Path installTmp = home.resolve(RELEASES_DIR).resolve("." + version + ".installing-" + Instant.now().toEpochMilli());
            copyDirectory(stagingDir, installTmp);
            replaceDirectory(installTmp, targetDir);

            pruneOldReleases(home);
            log.info("release package installed version={}", version);
            return toInfo(targetDir, version, currentVersion, resolveLinkVersion(home, PREVIOUS_LINK).orElse(null));
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID,
                    "发布包处理失败: " + ex.getMessage());
        } finally {
            deleteQuietly(tempZip);
            deleteQuietly(stagingDir);
        }
    }

    public ReleaseActivateResultVO activate(String version) {
        if (version == null || !VERSION_PATTERN.matcher(version.trim()).matches()) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_VERSION_INVALID,
                    "版本号必须为 MAJOR.MINOR.PATCH");
        }
        version = version.trim();
        Path home = requireHome();
        Path releaseDir = home.resolve(RELEASES_DIR).resolve(version);
        if (!Files.isDirectory(releaseDir) || !Files.isRegularFile(releaseDir.resolve(APP_JAR))) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_VERSION_NOT_FOUND, "版本不存在: " + version);
        }

        synchronized (activateLock) {
            String current = resolveLinkVersion(home, CURRENT_LINK).orElse(null);
            if (version.equals(current)) {
                throw new BusinessException(ErrorCodeConstants.RELEASE_ALREADY_CURRENT, "目标版本已是当前版本");
            }
            if (!restartScheduled.compareAndSet(false, true)) {
                throw new BusinessException(ErrorCodeConstants.RELEASE_ACTIVATE_CONFLICT, "重启已在进行中，请稍后");
            }
            try {
                if (current != null) {
                    switchLink(home, PREVIOUS_LINK, current);
                }
                switchLink(home, CURRENT_LINK, version);
                scheduleRestart();
                ReleaseActivateResultVO result = new ReleaseActivateResultVO();
                result.setVersion(version);
                result.setPreviousVersion(current);
                result.setRestartDelayMs(properties.getRestartDelayMs());
                result.setMessage("version switched; process will restart");
                log.info("release activated version={} previous={}", version, current);
                return result;
            } catch (RuntimeException ex) {
                restartScheduled.set(false);
                throw ex;
            } catch (IOException ex) {
                restartScheduled.set(false);
                throw new BusinessException(ErrorCodeConstants.RELEASE_HOME_UNAVAILABLE,
                        "切换版本失败: " + ex.getMessage());
            }
        }
    }

    public ReleaseActivateResultVO rollback() {
        Path home = requireHome();
        String previous = resolveLinkVersion(home, PREVIOUS_LINK)
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.RELEASE_PREVIOUS_NOT_FOUND,
                        "没有可回滚的 previous 版本"));
        return activate(previous);
    }

    private void scheduleRestart() {
        long delay = Math.max(0L, properties.getRestartDelayMs());
        restartScheduler.schedule(() -> {
            log.warn("release restart: exiting JVM in order to load new jar");
            try {
                restartAction.run();
            } catch (Throwable t) {
                log.error("restart action failed", t);
                restartScheduled.set(false);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private Path requireHome() {
        Path home = Path.of(properties.getHome()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(home);
            if (!Files.isWritable(home)) {
                throw new BusinessException(ErrorCodeConstants.RELEASE_HOME_UNAVAILABLE,
                        "发布根目录不可写: " + home);
            }
            return home;
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_HOME_UNAVAILABLE,
                    "发布根目录不可用: " + home + " (" + ex.getMessage() + ")");
        }
    }

    private void ensureLayout(Path home) {
        try {
            Files.createDirectories(home.resolve(RELEASES_DIR));
            Files.createDirectories(home.resolve(INCOMING_DIR));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_HOME_UNAVAILABLE,
                    "无法创建发布目录结构: " + ex.getMessage());
        }
    }

    private void extractAllowedZip(Path zipFile, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                // 仅允许根目录下的固定文件名
                Path namePath = Path.of(name).normalize();
                if (namePath.getNameCount() != 1 || namePath.startsWith("..")) {
                    throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID,
                            "zip 含非法路径: " + name);
                }
                String simpleName = namePath.getFileName().toString();
                if (!ALLOWED_ZIP_ENTRIES.contains(simpleName)) {
                    throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID,
                            "zip 含不允许的文件: " + simpleName);
                }
                Path out = targetDir.resolve(simpleName).normalize();
                if (!out.startsWith(targetDir)) {
                    throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID,
                            "zip 路径穿越: " + name);
                }
                try (OutputStream os = Files.newOutputStream(out)) {
                    zis.transferTo(os);
                }
                count++;
            }
            if (count == 0) {
                throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID, "zip 为空");
            }
        }
    }

    private void validateMeta(ReleaseVersionMeta meta) {
        if (meta == null || meta.getVersion() == null || meta.getVersion().isBlank()) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID, "version.json 缺少 version");
        }
        String version = meta.getVersion().trim();
        if (!VERSION_PATTERN.matcher(version).matches()) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_VERSION_INVALID,
                    "版本号必须为 MAJOR.MINOR.PATCH: " + version);
        }
        if (meta.getJarSha256() == null || meta.getJarSha256().isBlank()) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_PACKAGE_INVALID, "version.json 缺少 jarSha256");
        }
        meta.setVersion(version);
    }

    private void switchLink(Path home, String linkName, String version) throws IOException {
        Path link = home.resolve(linkName);
        Path target = home.resolve(RELEASES_DIR).resolve(version);
        if (!Files.isDirectory(target)) {
            throw new BusinessException(ErrorCodeConstants.RELEASE_VERSION_NOT_FOUND, "版本目录不存在: " + version);
        }
        Path tmp = home.resolve("." + linkName + ".tmp-" + Instant.now().toEpochMilli());
        Files.deleteIfExists(tmp);
        Files.createSymbolicLink(tmp, Path.of(RELEASES_DIR, version));
        try {
            Files.move(tmp, link, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.deleteIfExists(link);
            Files.move(tmp, link, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private Optional<String> resolveLinkVersion(Path home, String linkName) {
        Path link = home.resolve(linkName);
        if (!Files.exists(link)) {
            return Optional.empty();
        }
        try {
            Path target = Files.isSymbolicLink(link) ? Files.readSymbolicLink(link) : link;
            Path resolved = home.resolve(target).normalize();
            String name = resolved.getFileName() == null ? null : resolved.getFileName().toString();
            if (name != null && VERSION_PATTERN.matcher(name).matches()) {
                return Optional.of(name);
            }
            // 兼容指向 releases/x.y.z 的相对/绝对路径
            Path parent = resolved.getParent();
            if (parent != null && RELEASES_DIR.equals(parent.getFileName().toString())
                    && VERSION_PATTERN.matcher(resolved.getFileName().toString()).matches()) {
                return Optional.of(resolved.getFileName().toString());
            }
            return Optional.empty();
        } catch (IOException ex) {
            log.warn("failed to resolve link {}", link, ex);
            return Optional.empty();
        }
    }

    private ReleaseInfoVO toInfo(Path dir, String version, String current, String previous) throws IOException {
        ReleaseInfoVO vo = new ReleaseInfoVO();
        vo.setVersion(version);
        vo.setCurrent(Objects.equals(version, current));
        vo.setPrevious(Objects.equals(version, previous));
        Path jar = dir.resolve(APP_JAR);
        if (Files.isRegularFile(jar)) {
            vo.setJarSizeBytes(Files.size(jar));
        }
        ReleaseVersionMeta meta = readMeta(dir);
        if (meta != null) {
            vo.setBuildTime(meta.getBuildTime());
            vo.setGitCommit(meta.getGitCommit());
            vo.setJarSha256(meta.getJarSha256());
        }
        return vo;
    }

    private ReleaseVersionMeta readMeta(Path dir) throws IOException {
        Path metaFile = dir.resolve(VERSION_JSON);
        if (!Files.isRegularFile(metaFile)) {
            return null;
        }
        return objectMapper.readValue(Files.readString(metaFile, StandardCharsets.UTF_8), ReleaseVersionMeta.class);
    }

    private void pruneOldReleases(Path home) throws IOException {
        int keep = Math.max(1, properties.getKeepCount());
        String current = resolveLinkVersion(home, CURRENT_LINK).orElse(null);
        String previous = resolveLinkVersion(home, PREVIOUS_LINK).orElse(null);
        Path releases = home.resolve(RELEASES_DIR);
        if (!Files.isDirectory(releases)) {
            return;
        }
        List<Path> dirs;
        try (Stream<Path> stream = Files.list(releases)) {
            dirs = stream.filter(Files::isDirectory)
                    .filter(p -> VERSION_PATTERN.matcher(p.getFileName().toString()).matches())
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString(), this::compareVersionDesc))
                    .toList();
        }
        int kept = 0;
        for (Path dir : dirs) {
            String v = dir.getFileName().toString();
            if (v.equals(current) || v.equals(previous)) {
                continue;
            }
            kept++;
            if (kept > keep) {
                log.info("pruning old release {}", v);
                deleteRecursively(dir);
            }
        }
    }

    private int compareVersionDesc(String a, String b) {
        return compareVersion(b, a);
    }

    private int compareVersion(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        for (int i = 0; i < 3; i++) {
            int na = Integer.parseInt(pa[i]);
            int nb = Integer.parseInt(pb[i]);
            if (na != nb) {
                return Integer.compare(na, nb);
            }
        }
        return 0;
    }

    private static String sha256Hex(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file);
                DigestInputStream din = new DigestInputStream(in, digest)) {
            din.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String resolveRunningVersion() {
        Package pkg = ReleaseApplicationService.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null) {
            return pkg.getImplementationVersion();
        }
        return Optional.ofNullable(System.getProperty("app.running.version")).orElse("unknown");
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);
                Files.createDirectories(target.resolve(rel));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                Files.copy(file, target.resolve(rel), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void replaceDirectory(Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            Path backup = target.getParent().resolve("." + target.getFileName() + ".bak-" + Instant.now().toEpochMilli());
            Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(source, target);
                deleteRecursively(backup);
            } catch (IOException ex) {
                // 尽量恢复
                if (!Files.exists(target) && Files.exists(backup)) {
                    Files.move(backup, target);
                }
                throw ex;
            }
        } else {
            Files.move(source, target);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                deleteRecursively(path);
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            // ignore cleanup errors
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
