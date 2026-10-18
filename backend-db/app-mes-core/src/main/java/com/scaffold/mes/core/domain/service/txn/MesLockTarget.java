package com.scaffold.mes.core.domain.service.txn;

/**
 * MesLockTarget 一行要锁的聚合根
 * <p>
 * kind 取 WORK_ORDER、EQUIPMENT、LOT、WAFER、OTHER。同一 kind 内按 id 升序。
 *
 * @param kind 锁序类别
 * @param id 行主键
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record MesLockTarget(String kind, long id) {
}
