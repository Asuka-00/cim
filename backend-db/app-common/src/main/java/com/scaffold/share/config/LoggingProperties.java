package com.scaffold.share.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * LoggingProperties配置类
 * <p>
 * 负责提供LoggingProperties相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@ConfigurationProperties(prefix = "logging")
public class LoggingProperties {

    /** 是否启用审计日志 */
    private boolean auditEnabled = true;

    /** 是否启用性能日志 */
    private boolean performanceEnabled = true;

    /** 异步队列大小 */
    private int asyncQueueSize = 512;

    /** 日志文件路径 */
    private String filePath = "logs/";

    /** 单个日志文件最大大小 */
    private String fileMaxSize = "100MB";

    /** 日志文件保留天数 */
    private int fileMaxHistory = 30;

    /** 是否启用脱敏 */
    private boolean maskingEnabled = true;

    /**
     * 控制台最低输出级别，由 {@code logback-spring.xml} 读取（dev 为 DEBUG，其余默认 WARN）。
     * 控制台格式由 profile 决定：dev 为文本，非 dev 为 JSON。
     */
    private String consoleThreshold = "WARN";

    /** 应用运行日志文件名 */
    private String appLogFile = "app.log";

    /** 审计日志文件名 */
    private String auditLogFile = "audit.log";

    /** SQL 日志文件名 */
    private String sqlLogFile = "sql.log";

    /** 错误日志文件名 */
    private String errorLogFile = "error.log";

    /** 性能日志文件名 */
    private String performanceLogFile = "performance.log";
}
