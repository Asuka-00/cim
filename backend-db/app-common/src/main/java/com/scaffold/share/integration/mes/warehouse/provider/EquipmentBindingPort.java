package com.scaffold.share.integration.mes.warehouse.provider;

import java.math.BigDecimal;

import com.scaffold.share.integration.mes.warehouse.response.BindingDeduction;

/**
 * EquipmentBindingPort 设备物料绑定
 * <p>
 * 执行模块不能直接改绑定剩余量。没有够扣的绑定时空。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EquipmentBindingPort {

    /**
     * 从设备上的绑定扣减。
     *
     * @param equipmentId 设备
     * @param materialId 物料
     * @param qty 数量
     * @return 扣到的绑定。没有或剩余不够时为空
     */
    BindingDeduction deduct(Long equipmentId, Long materialId, BigDecimal qty);

    /**
     * 这个绑定是否已按报废下料。
     *
     * @param bindingId 绑定。0 或空表示没有绑定
     * @return 已报废下料时为 true
     */
    boolean scrapped(Long bindingId);
}
