package com.scaffold.share.integration.mes.warehouse.response;

/**
 * BindingDeduction 一次从设备绑定扣到的物料
 *
 * @param bindingId 绑定
 * @param materialLotId 物料批
 * @param uomId 计量单位
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record BindingDeduction(Long bindingId, Long materialLotId, Long uomId) {
}
