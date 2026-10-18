package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * UomConversionSaveRequest 物料单位换算保存请求
 * <p>
 * 新增与修改共用。未传物料时按通用换算（materialId=0）保存。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class UomConversionSaveRequest {

    /** 源单位。 */
    @NotNull
    private Long fromUomId;

    /** 目标单位。 */
    @NotNull
    private Long toUomId;

    /** 限定物料。空则按通用换算。 */
    private Long materialId;

    /** 换算系数，必须大于 0。 */
    @NotNull
    private BigDecimal factor;
}
