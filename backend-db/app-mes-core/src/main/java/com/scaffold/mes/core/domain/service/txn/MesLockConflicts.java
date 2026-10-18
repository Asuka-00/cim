package com.scaffold.mes.core.domain.service.txn;

import java.sql.SQLException;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.share.exception.BusinessException;

/**
 * MesLockConflicts 行锁冲突翻译
 * <p>
 * 聚合根使用 {@code FOR UPDATE NOWAIT}。MySQL 3572 立刻变成「对象被占用」,1205 是短等待超时。
 * 真正的加锁语句由各业务事务在持有行之后发出,本类只翻译错误。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public final class MesLockConflicts {

    /** 聚合根行锁子句。不要用于计数器、库存这些允许短等待的行。 */
    public static final String NOWAIT = "FOR UPDATE NOWAIT";

    private MesLockConflicts() {
    }

    /**
     * 把锁异常译成业务错误。不是锁冲突时返回 null。
     *
     * @param error 原始异常,会沿着 cause 查找 SQLException
     * @return 400102 或 400103,没有锁错误时为 null
     */
    public static BusinessException translate(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sql) {
                if (sql.getErrorCode() == 3572) {
                    return new BusinessException(MesErrorCodes.OBJECT_BUSY, "对象被占用");
                }
                if (sql.getErrorCode() == 1205) {
                    return new BusinessException(MesErrorCodes.LOCK_WAIT_TIMEOUT, "锁等待超时");
                }
            }
            current = current.getCause();
        }
        return null;
    }
}
