package com.scaffold.system.domain.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;

/**
 * LogFileService 日志文件服务。
 * <p>
 * 负责日志目录解析、文件白名单校验、尾部读取和下载资源构建。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Service
public class LogFileService {

    private static final int DEFAULT_TAIL_LINES = 500;
    private static final int MAX_TAIL_LINES = 5000;
    private static final long DEFAULT_FULL_PREVIEW_MAX_BYTES = 104857600L;
    private static final String ALL_LINES = "ALL";
    private static final String LOG_PATH_ENV = "LOG_PATH";

    @Value("${logging.file-path:logs/}")
    private String loggingFilePath;

    @Value("${logging.log-file-preview.max-bytes:104857600}")
    private long fullPreviewMaxBytes = DEFAULT_FULL_PREVIEW_MAX_BYTES;

    public LogFileService() {
    }

    LogFileService(String loggingFilePath) {
        this(loggingFilePath, DEFAULT_FULL_PREVIEW_MAX_BYTES);
    }

    LogFileService(String loggingFilePath, long fullPreviewMaxBytes) {
        this.loggingFilePath = loggingFilePath;
        this.fullPreviewMaxBytes = fullPreviewMaxBytes;
    }

    public List<LogFileInfoVO> listFiles(String typeCode) {
        LogFileType type = LogFileType.fromCode(typeCode);
        Path logDirectory = resolveLogDirectory();
        if (!Files.isDirectory(logDirectory)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(logDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> type.matches(path.getFileName().toString()))
                    .map(path -> toInfo(type, path))
                    .sorted(Comparator.comparing(LogFileInfoVO::getLastModifiedAt).reversed()
                            .thenComparing(LogFileInfoVO::getFileName))
                    .toList();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "读取日志文件列表失败", ex);
        }
    }

    public LogTailVO tail(String typeCode, String fileName, Integer requestedLines) {
        return tail(typeCode, fileName, requestedLines == null ? null : String.valueOf(requestedLines));
    }

    public LogTailVO tail(String typeCode, String fileName, String requestedLines) {
        LogFileType type = LogFileType.fromCode(typeCode);
        Path file = resolveAllowedFile(type, fileName);
        if (isCompressed(file)) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "压缩日志不支持在线查看，请下载后查看");
        }

        if (isAllLines(requestedLines)) {
            return readAll(type, file);
        }

        int lines = normalizeLines(requestedLines);
        ArrayDeque<String> tailLines = new ArrayDeque<>(lines);
        try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
            stream.forEach(line -> {
                if (tailLines.size() == lines) {
                    tailLines.removeFirst();
                }
                tailLines.addLast(line);
            });
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "读取日志文件失败", ex);
        }
        return new LogTailVO(file.getFileName().toString(), type.getCode(), lines, List.copyOf(tailLines));
    }

    private LogTailVO readAll(LogFileType type, Path file) {
        ensureFullPreviewAllowed(file);
        try (Stream<String> stream = Files.lines(file, StandardCharsets.UTF_8)) {
            List<String> content = stream.toList();
            return new LogTailVO(file.getFileName().toString(), type.getCode(), content.size(), content);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "读取日志文件失败", ex);
        }
    }

    public Resource download(String typeCode, String fileName) {
        LogFileType type = LogFileType.fromCode(typeCode);
        return new PathResource(resolveAllowedFile(type, fileName));
    }

    public long contentLength(Resource resource) {
        try {
            return resource.contentLength();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "读取日志文件大小失败", ex);
        }
    }

    private LogFileInfoVO toInfo(LogFileType type, Path path) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            LocalDateTime lastModifiedAt = LocalDateTime.ofInstant(lastModified.toInstant(), ZoneId.systemDefault());
            return new LogFileInfoVO(path.getFileName().toString(), type.getCode(), Files.size(path), lastModifiedAt,
                    isCompressed(path));
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "读取日志文件信息失败", ex);
        }
    }

    private Path resolveAllowedFile(LogFileType type, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "日志文件名不能为空");
        }
        if (!type.matches(fileName)) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "非法日志文件名: " + fileName);
        }

        Path fileNamePath;
        try {
            fileNamePath = Paths.get(fileName);
        } catch (InvalidPathException ex) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "非法日志文件名: " + fileName, ex);
        }
        if (fileNamePath.isAbsolute() || fileNamePath.getNameCount() != 1) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "非法日志文件名: " + fileName);
        }

        Path logDirectory = resolveLogDirectory();
        Path file = logDirectory.resolve(fileName).normalize();
        if (!file.startsWith(logDirectory)) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "非法日志文件路径: " + fileName);
        }
        if (!Files.isRegularFile(file)) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "日志文件不存在: " + fileName);
        }
        ensureRealPathInsideLogDirectory(file, logDirectory, fileName);
        return file;
    }

    private void ensureRealPathInsideLogDirectory(Path file, Path logDirectory, String fileName) {
        try {
            if (!file.toRealPath().startsWith(logDirectory.toRealPath())) {
                throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "非法日志文件路径: " + fileName);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "校验日志文件路径失败", ex);
        }
    }

    private Path resolveLogDirectory() {
        String logPath = System.getenv(LOG_PATH_ENV);
        if (logPath == null || logPath.isBlank()) {
            logPath = loggingFilePath;
        }
        if (logPath == null || logPath.isBlank()) {
            logPath = "logs/";
        }
        return Paths.get(logPath).toAbsolutePath().normalize();
    }

    private int normalizeLines(String requestedLines) {
        if (requestedLines == null || requestedLines.isBlank()) {
            return DEFAULT_TAIL_LINES;
        }
        int lines;
        try {
            lines = Integer.parseInt(requestedLines);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "日志行数必须为正整数或ALL", ex);
        }
        if (lines <= 0) {
            throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "日志行数必须大于0");
        }
        return Math.min(lines, MAX_TAIL_LINES);
    }

    private boolean isAllLines(String requestedLines) {
        return requestedLines != null && ALL_LINES.equalsIgnoreCase(requestedLines.trim());
    }

    private void ensureFullPreviewAllowed(Path file) {
        try {
            if (Files.size(file) > fullPreviewMaxBytes) {
                throw new BusinessException(ErrorCodeConstants.INVALID_PARAM, "日志文件过大，请下载后查看");
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCodeConstants.SYSTEM_ERROR, "读取日志文件大小失败", ex);
        }
    }

    private boolean isCompressed(Path path) {
        return path.getFileName().toString().endsWith(".gz");
    }
}
