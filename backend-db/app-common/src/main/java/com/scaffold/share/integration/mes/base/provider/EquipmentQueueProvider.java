package com.scaffold.share.integration.mes.base.provider;

import java.util.List;

/**
 * EquipmentQueueProvider 设备能加工的站点
 * <p>
 * 由 app-mes-base 实现。作业清单按设备过滤时，只保留这些站点上的待加工 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentQueueProvider {

    /**
     * 设备所属设备组绑定的站点。
     *
     * @param equipmentId 设备
     * @return 站点主键，设备没有设备组时为空列表
     */
    List<Long> stepIds(Long equipmentId);
}
