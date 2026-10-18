package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

/**
 * ProductUnitConvCheck 产品计数换算核对结果
 * <p>
 * referenceQty 是按系数算出的参考数量。deviationPct 是实际数量相对参考数量的绝对偏差百分比。
 *
 * @param referenceQty 参考数量
 * @param deviationPct 偏差百分比
 * @param withinTolerance 偏差未超过允许百分比时为 true
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record ProductUnitConvCheck(BigDecimal referenceQty, BigDecimal deviationPct, boolean withinTolerance) {
}
