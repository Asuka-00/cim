package com.scaffold.share.integration.mes.warehouse.response;

/**
 * StockDeduction 一次扣料落到的库存
 *
 * @param materialLotId 物料批
 * @param locationId 库位
 * @param uomId 计量单位
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record StockDeduction(Long materialLotId, Long locationId, Long uomId) {
}
