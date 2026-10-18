package com.scaffold.system.domain.service.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.config.ReleaseProperties;
import com.scaffold.system.domain.vo.system.ReleaseActivateResultVO;
import com.scaffold.system.domain.vo.system.ReleaseInfoVO;

class ReleaseApplicationServiceTest {

    @TempDir
    Path tempDir;

    private ReleaseProperties properties;
    private AtomicInteger restartCount;
    private ReleaseApplicationService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new ReleaseProperties();
        properties.setHome(tempDir.toString());
        properties.setMaxUploadBytes(10 * 1024 * 1024);
        properties.setRestartDelayMs(0L);
        properties.setKeepCount(2);
        restartCount = new AtomicInteger();
        service = newService();
    }

    private ReleaseApplicationService newService() {
        return ReleaseApplicationService.createForTest(properties, objectMapper, restartCount::incrementAndGet);
    }

    @Test
    void uploadValidPackageSucceeds() throws Exception {
        MockMultipartFile file = zipOf("1.0.0", "hello-jar");
        ReleaseInfoVO info = service.upload(file);
        assertThat(info.getVersion()).isEqualTo("1.0.0");
        assertThat(Files.isRegularFile(tempDir.resolve("releases/1.0.0/app.jar"))).isTrue();
        assertThat(service.listReleases()).hasSize(1);
    }

    @Test
    void uploadRejectsInvalidVersion() throws Exception {
        MockMultipartFile file = zipOf("1.0", "x");
        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.RELEASE_VERSION_INVALID);
    }

    @Test
    void uploadRejectsChecksumMismatch() throws Exception {
        byte[] jar = "payload".getBytes(StandardCharsets.UTF_8);
        String meta = """
                {"version":"1.0.1","buildTime":"t","jarSha256":"deadbeef"}
                """;
        MockMultipartFile file = zipBytes(jar, meta);
        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.RELEASE_CHECKSUM_MISMATCH);
    }

    @Test
    void uploadRejectsZipSlipAndExtraFiles() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry("../evil.jar"));
            zos.write(new byte[] {1});
            zos.closeEntry();
        }
        MockMultipartFile file = new MockMultipartFile("file", "bad.zip", "application/zip", bos.toByteArray());
        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.RELEASE_PACKAGE_INVALID);
    }

    @Test
    void uploadAllowsOverwriteNonCurrent() throws Exception {
        service.upload(zipOf("1.0.0", "v1"));
        service.upload(zipOf("1.0.1", "v2"));
        service.activate("1.0.0");
        // 覆盖非 current 的 1.0.1
        ReleaseInfoVO again = service.upload(zipOf("1.0.1", "v2-new"));
        assertThat(again.getVersion()).isEqualTo("1.0.1");
        assertThat(Files.readString(tempDir.resolve("releases/1.0.1/app.jar"))).isEqualTo("v2-new");
    }

    @Test
    void uploadRejectsOverwriteCurrent() throws Exception {
        service.upload(zipOf("1.0.0", "v1"));
        service.activate("1.0.0");
        assertThatThrownBy(() -> service.upload(zipOf("1.0.0", "v1-new")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.RELEASE_CURRENT_OVERWRITE_FORBIDDEN);
    }

    @Test
    void activateUpdatesCurrentAndPreviousAndSchedulesRestart() throws Exception {
        service.upload(zipOf("1.0.0", "a"));
        service.upload(zipOf("1.0.1", "b"));
        service.activate("1.0.0");
        waitRestart();
        assertThat(service.getCurrent().getCurrentVersion()).isEqualTo("1.0.0");

        // 模拟进程重启后新实例再激活
        service = newService();
        ReleaseActivateResultVO result = service.activate("1.0.1");
        assertThat(result.getVersion()).isEqualTo("1.0.1");
        assertThat(result.getPreviousVersion()).isEqualTo("1.0.0");
        waitRestart();
        assertThat(service.getCurrent().getCurrentVersion()).isEqualTo("1.0.1");
        assertThat(service.getCurrent().getPreviousVersion()).isEqualTo("1.0.0");
        assertThat(restartCount.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void rollbackUsesPrevious() throws Exception {
        service.upload(zipOf("1.0.0", "a"));
        service.upload(zipOf("1.0.1", "b"));
        service.activate("1.0.0");
        waitRestart();
        service = newService();
        service.activate("1.0.1");
        waitRestart();
        service = newService();
        ReleaseActivateResultVO rolled = service.rollback();
        assertThat(rolled.getVersion()).isEqualTo("1.0.0");
        waitRestart();
        assertThat(service.getCurrent().getCurrentVersion()).isEqualTo("1.0.0");
    }

    @Test
    void rollbackFailsWithoutPrevious() {
        assertThatThrownBy(() -> service.rollback())
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ErrorCodeConstants.RELEASE_PREVIOUS_NOT_FOUND);
    }

    @Test
    void pruneKeepsCurrentPreviousAndLimit() throws Exception {
        // 先装多个版本并建立 current/previous，再收紧 keepCount 触发裁剪
        service.upload(zipOf("1.0.0", "a"));
        service.upload(zipOf("1.0.1", "b"));
        service.activate("1.0.0");
        waitRestart();
        service = newService();
        service.activate("1.0.1");
        waitRestart();

        properties.setKeepCount(1);
        service = newService();
        service.upload(zipOf("1.0.2", "c"));
        service.upload(zipOf("1.0.3", "d"));
        // current=1.0.1 previous=1.0.0 keep=1 -> 非保护版本仅保留最新 1.0.3
        List<String> versions = service.listReleases().stream().map(ReleaseInfoVO::getVersion).toList();
        assertThat(versions).contains("1.0.1", "1.0.0", "1.0.3");
        assertThat(versions).doesNotContain("1.0.2");
    }

    private void waitRestart() throws InterruptedException {
        Thread.sleep(50);
    }

    private MockMultipartFile zipOf(String version, String jarContent) throws Exception {
        byte[] jar = jarContent.getBytes(StandardCharsets.UTF_8);
        String sha = sha256(jar);
        String meta = """
                {"version":"%s","buildTime":"2026-08-12T00:00:00Z","gitCommit":"test","jarSha256":"%s"}
                """.formatted(version, sha);
        return zipBytes(jar, meta);
    }

    private MockMultipartFile zipBytes(byte[] jar, String metaJson) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry(ReleaseApplicationService.APP_JAR));
            zos.write(jar);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(ReleaseApplicationService.VERSION_JSON));
            zos.write(metaJson.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return new MockMultipartFile("file", "app.zip", "application/zip", bos.toByteArray());
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }
}
