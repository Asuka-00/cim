package com.scaffold.system.domain.log;

import java.util.Arrays;
import java.util.regex.Pattern;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;

/**
 * LogFileType 日志文件类型
 * <p>
 * 定义前端可访问的日志文件白名单类型及文件名规则。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
public enum LogFileType {

    APP("app", "app", Pattern.compile("^app(?:\\.\\d{4}-\\d{2}-\\d{2}\\.\\d+\\.log\\.gz|\\.log)$")),
    AUDIT("audit", "audit", Pattern.compile("^audit(?:\\.\\d{4}-\\d{2}-\\d{2}\\.\\d+\\.log\\.gz|\\.log)$")),
    SQL("sql", "sql", Pattern.compile("^sql(?:\\.\\d{4}-\\d{2}-\\d{2}\\.\\d+\\.log\\.gz|\\.log)$")),
    ERROR("error", "error", Pattern.compile("^error(?:\\.\\d{4}-\\d{2}-\\d{2}\\.\\d+\\.log\\.gz|\\.log)$")),
    SLOW("slow", "performance",
            Pattern.compile("^performance(?:\\.\\d{4}-\\d{2}-\\d{2}\\.\\d+\\.log\\.gz|\\.log)$"));

    private final String code;
    private final String filePrefix;
    private final Pattern fileNamePattern;

    LogFileType(String code, String filePrefix, Pattern fileNamePattern) {
        this.code = code;
        this.filePrefix = filePrefix;
        this.fileNamePattern = fileNamePattern;
    }

    public String getCode() {
        return code;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public boolean matches(String fileName) {
        return fileName != null && fileNamePattern.matcher(fileName).matches();
    }

    public static LogFileType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodeConstants.INVALID_PARAM,
                        "不支持的日志类型: " + code));
    }
}
