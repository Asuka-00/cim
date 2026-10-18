package com.scaffold.share.integration.mes.base.provider;

/**
 * EquipmentGroupPort 设备所属设备组
 * <p>
 * 执行模块不能直接读设备表。没有设备组时返回空。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentGroupPort {

    /**
     * 设备所属的设备组。
     *
     * @param equipmentId 设备
     * @return 设备组，设备不存在或未分组时为空
     */
    Long groupId(Long equipmentId);
}
