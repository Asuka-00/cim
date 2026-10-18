package com.scaffold.share.integration.mes.base.response;

import java.math.BigDecimal;

/**
 * UnitConvertFacts 当前步骤的单位转换
 * <p>
 * 不是转换站点时 convertStep 为 false，其余字段为空。
 *
 * @param convertStep 当前步骤是否单位转换
 * @param toUnitId 转换后单位
 * @param factor 1 个源单位等于多少目标单位
 * @param tolerancePct 允许偏差百分比
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record UnitConvertFacts(boolean convertStep, Long toUnitId, BigDecimal factor, BigDecimal tolerancePct) {

    /**
     * 当前步骤不转换单位。
     *
     * @return 空事实
     */
    public static UnitConvertFacts none() {
        return new UnitConvertFacts(false, null, null, null);
    }
}
