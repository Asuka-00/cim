package com.scaffold.mes.core.domain.service.txn;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.share.exception.BusinessException;

/**
 * NOWAIT 冲突要立刻失败,等待超时单独报错。
 */
class MesLockConflictsTest {

    @Test
    void nowaitConflictIsImmediateBusy() {
        SQLException mysql = new SQLException("nowait", "HY000", 3572);
        BusinessException translated = MesLockConflicts.translate(new RuntimeException(mysql));

        assertThat(translated.getCode()).isEqualTo(MesErrorCodes.OBJECT_BUSY);
        assertThat(translated.getMessage()).isEqualTo("对象被占用");
        assertThat(MesLockConflicts.NOWAIT).isEqualTo("FOR UPDATE NOWAIT");
    }

    @Test
    void lockWaitTimeoutIsSeparate() {
        BusinessException translated = MesLockConflicts.translate(new SQLException("wait", "HY000", 1205));

        assertThat(translated.getCode()).isEqualTo(MesErrorCodes.LOCK_WAIT_TIMEOUT);
    }

    @Test
    void unrelatedErrorIsLeftAlone() {
        assertThat(MesLockConflicts.translate(new SQLException("other", "HY000", 1062))).isNull();
    }
}
