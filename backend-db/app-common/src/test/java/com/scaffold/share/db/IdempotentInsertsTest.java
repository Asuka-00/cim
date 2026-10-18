package com.scaffold.share.db;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import com.scaffold.share.infra.mapper.BatchBaseMapper;

class IdempotentInsertsTest {

    @SuppressWarnings("unchecked")
    private final BatchBaseMapper<String> mapper = mock(BatchBaseMapper.class);

    @Test
    void primaryKeyConflictOnSingleInsertIsSkipped() {
        doThrow(primaryKeyConflict()).when(mapper).insert("a");

        IdempotentInserts.insert(mapper, "a");

        verify(mapper).insert("a");
    }

    @Test
    void businessUniqueKeyConflictStillFails() {
        doThrow(new DuplicateKeyException("dup", new SQLIntegrityConstraintViolationException(
                "Duplicate entry 'admin' for key 'sys_user.uk_user_name'"))).when(mapper).insert("a");

        assertThrows(DuplicateKeyException.class, () -> IdempotentInserts.insert(mapper, "a"));
    }

    @Test
    void batchWithPrimaryConflictFallsBackToRowByRow() {
        doThrow(primaryKeyConflict()).when(mapper).insertBatchSomeColumn(anyCollection());
        doThrow(primaryKeyConflict()).when(mapper).insert("replayed");

        IdempotentInserts.insertAll(mapper, List.of("replayed", "fresh"));

        verify(mapper).insert("replayed");
        verify(mapper).insert("fresh");
    }

    @Test
    void batchWithOtherErrorPropagates() {
        doThrow(new IllegalStateException("db down")).when(mapper).insertBatchSomeColumn(anyCollection());

        assertThrows(IllegalStateException.class, () -> IdempotentInserts.insertAll(mapper, List.of("a")));
    }

    private static DuplicateKeyException primaryKeyConflict() {
        return new DuplicateKeyException("dup", new SQLIntegrityConstraintViolationException(
                "Duplicate entry '1' for key 'sys_user.PRIMARY'"));
    }
}
