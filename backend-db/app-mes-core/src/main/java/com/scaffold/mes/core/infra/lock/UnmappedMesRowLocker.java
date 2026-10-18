package com.scaffold.mes.core.infra.lock;

import org.springframework.stereotype.Component;

import com.scaffold.mes.core.domain.service.txn.MesRowLocker;

/**
 * UnmappedMesRowLocker 尚未映射到表的行锁
 * <p>
 * M0 还没有 Lot、设备这些业务表。事务如果声明了要锁的行,在这里失败,避免假装已经锁上。
 * M3 换成按表执行 {@code FOR UPDATE NOWAIT} 的实现。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
public class UnmappedMesRowLocker implements MesRowLocker {

    /**
     * 拒绝加锁。
     *
     * @param kind 锁序类别
     * @param id 行主键
     */
    @Override
    public void lockNowait(String kind, long id) {
        throw new IllegalStateException("行锁还没有对应的表: " + kind + "#" + id);
    }
}
