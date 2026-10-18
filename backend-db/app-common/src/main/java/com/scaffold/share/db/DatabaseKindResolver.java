package com.scaffold.share.db;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

/**
 * DatabaseKindResolver 从 JDBC URL 推断数据库种类
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2025-01-29
 */
@Slf4j
public final class DatabaseKindResolver {

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("^jdbc:([^:]+):");

    private DatabaseKindResolver() {
    }

    /**
     * 从 JDBC URL 解析数据库种类。
     *
     * @param jdbcUrl     数据源 URL
     * @param explicitType 显式配置的 {@code app.db.type}，优先级高于 URL 推断
     * @return 数据库种类
     * @throws IllegalArgumentException URL 无法识别且未配置 explicitType
     */
    public static DatabaseKind resolve(String jdbcUrl, String explicitType) {
        if (explicitType != null && !explicitType.isBlank()) {
            log.info("using explicit app.db.type={}", explicitType);
            return DatabaseKind.fromConfigValue(explicitType);
        }

        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("jdbc URL 为空且未配置 app.db.type");
        }

        Matcher matcher = JDBC_URL_PATTERN.matcher(jdbcUrl.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("无法解析 jdbc URL: " + jdbcUrl);
        }

        String protocol = matcher.group(1).toLowerCase();
        DatabaseKind kind = switch (protocol) {
            case "mysql" -> DatabaseKind.MYSQL;
            case "postgresql" -> DatabaseKind.POSTGRESQL;
            case "oracle" -> DatabaseKind.ORACLE;
            case "sqlserver" -> DatabaseKind.SQLSERVER;
            default -> null;
        };

        if (kind == null) {
            throw new IllegalArgumentException(
                    "不支持的数据库类型: " + protocol + "（从 URL: " + jdbcUrl + "），请配置 app.db.type");
        }

        log.info("resolved database kind from URL: {}", kind);
        return kind;
    }
}
