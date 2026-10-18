package com.scaffold.share.integration.mes.base.response;

import java.math.BigDecimal;

/**
 * BomConsumeLine 出站要扣的一行 BOM
 * <p>
 * 只包含 PER_LOT 且打开反冲的行。
 *
 * @param materialId 物料
 * @param qtyPer 每批用量
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record BomConsumeLine(Long materialId, BigDecimal qtyPer) {
}
