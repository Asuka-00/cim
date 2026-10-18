package com.scaffold.share.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

class DuplicateKeysTest {

    @Test
    void mysqlPrimaryKeyInCauseChain() {
        SQLIntegrityConstraintViolationException root = new SQLIntegrityConstraintViolationException(
                "Duplicate entry '96081512390668' for key 'sys_user.PRIMARY'", "23000", 1062);
        DuplicateKeyException translated = new DuplicateKeyException(
                "### Error updating database. Cause: " + root.getMessage(), root);

        assertTrue(DuplicateKeys.isPrimaryKeyViolation(new RuntimeException("wrapped", translated)));
    }

    @Test
    void mysqlLegacyPrimaryKeyName() {
        assertTrue(DuplicateKeys.isPrimaryKeyViolation(new SQLIntegrityConstraintViolationException(
                "Duplicate entry '1' for key 'PRIMARY'")));
    }

    @Test
    void mysqlBusinessUniqueKeyIsNotPrimary() {
        SQLIntegrityConstraintViolationException root = new SQLIntegrityConstraintViolationException(
                "Duplicate entry 'admin' for key 'sys_user.uk_user_name'");

        assertFalse(DuplicateKeys.isPrimaryKeyViolation(new DuplicateKeyException("dup", root)));
    }

    @Test
    void postgresAndSqlServerAndOraclePrimaryKeys() {
        assertTrue(DuplicateKeys.isPrimaryKeyViolation(new SQLException(
                "ERROR: duplicate key value violates unique constraint \"sys_user_pkey\"", "23505")));
        assertTrue(DuplicateKeys.isPrimaryKeyViolation(new SQLException(
                "Violation of PRIMARY KEY constraint 'PK__sys_user__3213E83F'.", "23000", 2627)));
        assertTrue(DuplicateKeys.isPrimaryKeyViolation(new SQLIntegrityConstraintViolationException(
                "ORA-00001: unique constraint (APP.PK_SYS_USER) violated")));
        assertFalse(DuplicateKeys.isPrimaryKeyViolation(new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uk_user_name\"", "23505")));
    }

    @Test
    void nonDuplicateErrorsAreIgnored() {
        assertFalse(DuplicateKeys.isPrimaryKeyViolation(new DataIntegrityViolationException(
                "Column 'id' for key 'PRIMARY' cannot be null")));
        assertFalse(DuplicateKeys.isPrimaryKeyViolation(new IllegalStateException("db down")));
        assertFalse(DuplicateKeys.isPrimaryKeyViolation(null));
    }
}
