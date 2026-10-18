package com.scaffold.share.db;

import com.baomidou.mybatisplus.annotation.DbType;

/**
 * DatabaseKind 关系库种类
 * <p>
 * 现场用 JDBC URL 或 {@code app.db.type} 选择；分页、Flyway 目录与批量插入策略都问本枚举。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2025-01-29
 */
public enum DatabaseKind {

    MYSQL("mysql", "mysql", DbType.MYSQL, true),
    POSTGRESQL("postgresql", "postgresql", DbType.POSTGRE_SQL, true),
    ORACLE("oracle", "oracle", DbType.ORACLE, false),
    SQLSERVER("sqlserver", "sqlserver", DbType.SQL_SERVER, true);

    private final String configValue;
    private final String folder;
    private final DbType mpDbType;
    private final boolean multiValueInsert;

    DatabaseKind(String configValue, String folder, DbType mpDbType, boolean multiValueInsert) {
        this.configValue = configValue;
        this.folder = folder;
        this.mpDbType = mpDbType;
        this.multiValueInsert = multiValueInsert;
    }

    public String configValue() {
        return configValue;
    }

    /**
     * Flyway 脚本子目录名，对应 {@code classpath:db/migration/{folder}}。
     */
    public String folder() {
        return folder;
    }

    public DbType toMpDbType() {
        return mpDbType;
    }

    /**
     * MyBatis {@code databaseId}，供极少数原生 SQL 分支使用。
     */
    public String databaseId() {
        return configValue;
    }

    public boolean supportsMultiValueInsert() {
        return multiValueInsert;
    }

    public static DatabaseKind fromConfigValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "mysql", "mariadb" -> MYSQL;
            case "postgresql", "postgres", "pgsql" -> POSTGRESQL;
            case "oracle" -> ORACLE;
            case "sqlserver", "mssql", "sql-server" -> SQLSERVER;
            default -> throw new IllegalArgumentException("不支持的 app.db.type: " + raw
                    + "（允许 mysql / postgresql / oracle / sqlserver）");
        };
    }
}
