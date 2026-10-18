package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ProductUnitConvSaveRequest 产品计数换算保存请求
 * <p>
 * 新增与修改共用。未传允许偏差时按 5%。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ProductUnitConvSaveRequest {

    /** 源计数单位。 */
    @NotNull
    private Long fromUnitId;

    /** 目标计数单位。 */
    @NotNull
    private Long toUnitId;

    /** 换算系数，必须大于 0。 */
    @NotNull
    private BigDecimal factor;

    /** 允许偏差百分比。空则按 5。 */
    private BigDecimal tolerancePct;

    /** 备注。 */
    private String remark;
}
