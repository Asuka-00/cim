package com.scaffold.mes.core.domain.service.txn;

/**
 * MesRowLocker 事务内行锁
 * <p>
 * 实现必须使用 {@link MesLockConflicts#NOWAIT},冲突时抛出「对象被占用」,不能排队等待。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesRowLocker {

    /**
     * 锁住一行。调用方已按 {@link MesLockOrder} 排好序。
     *
     * @param kind 锁序类别
     * @param id 行主键
     */
    void lockNowait(String kind, long id);
}
