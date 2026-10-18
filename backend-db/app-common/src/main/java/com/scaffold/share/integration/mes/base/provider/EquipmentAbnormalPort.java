package com.scaffold.share.integration.mes.base.provider;

/**
 * EquipmentAbnormalPort 非计划停机的设备数量
 * <p>
 * 执行模块不能直接读设备表。统计 E10 状态为 UNSCHEDULED_DOWN 的设备。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentAbnormalPort {

    /**
     * 非计划停机的设备台数。
     *
     * @return 台数，没有时为 0
     */
    long count();
}
