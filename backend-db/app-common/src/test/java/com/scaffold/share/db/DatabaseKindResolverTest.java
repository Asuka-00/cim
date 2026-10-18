package com.scaffold.share.db;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * DatabaseKindResolver 测试
 *
 * @author Kairo
 */
class DatabaseKindResolverTest {

    @Test
    void testResolveMysql() {
        DatabaseKind kind = DatabaseKindResolver.resolve("jdbc:mysql://localhost:3306/test", null);
        assertEquals(DatabaseKind.MYSQL, kind);
    }

    @Test
    void testResolvePostgresql() {
        DatabaseKind kind = DatabaseKindResolver.resolve("jdbc:postgresql://localhost:5432/test", null);
        assertEquals(DatabaseKind.POSTGRESQL, kind);
    }

    @Test
    void testResolveOracle() {
        DatabaseKind kind = DatabaseKindResolver.resolve("jdbc:oracle:thin:@//localhost:1521/orcl", null);
        assertEquals(DatabaseKind.ORACLE, kind);
    }

    @Test
    void testResolveSqlServer() {
        DatabaseKind kind = DatabaseKindResolver.resolve("jdbc:sqlserver://localhost:1433;databaseName=test", null);
        assertEquals(DatabaseKind.SQLSERVER, kind);
    }

    @Test
    void testExplicitTypeOverridesUrl() {
        DatabaseKind kind = DatabaseKindResolver.resolve("jdbc:mysql://localhost:3306/test", "postgresql");
        assertEquals(DatabaseKind.POSTGRESQL, kind);
    }

    @Test
    void testExplicitTypeAlias() {
        assertEquals(DatabaseKind.MYSQL, DatabaseKind.fromConfigValue("mariadb"));
        assertEquals(DatabaseKind.POSTGRESQL, DatabaseKind.fromConfigValue("postgres"));
        assertEquals(DatabaseKind.POSTGRESQL, DatabaseKind.fromConfigValue("pgsql"));
        assertEquals(DatabaseKind.SQLSERVER, DatabaseKind.fromConfigValue("mssql"));
        assertEquals(DatabaseKind.SQLSERVER, DatabaseKind.fromConfigValue("sql-server"));
    }

    @Test
    void testInvalidUrl() {
        assertThrows(IllegalArgumentException.class, () -> 
            DatabaseKindResolver.resolve("invalid-url", null));
    }

    @Test
    void testUnsupportedProtocol() {
        assertThrows(IllegalArgumentException.class, () -> 
            DatabaseKindResolver.resolve("jdbc:h2:mem:test", null));
    }

    @Test
    void testEmptyUrl() {
        assertThrows(IllegalArgumentException.class, () -> 
            DatabaseKindResolver.resolve("", null));
    }

    @Test
    void testInvalidExplicitType() {
        assertThrows(IllegalArgumentException.class, () -> 
            DatabaseKind.fromConfigValue("unknown"));
    }
}
