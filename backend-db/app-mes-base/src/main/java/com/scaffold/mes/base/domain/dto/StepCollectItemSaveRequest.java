package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * StepCollectItemSaveRequest 保存站点采集规格
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StepCollectItemSaveRequest {

    /** 站点。 */
    @NotNull
    private Long stepId;

    /** 采集项。 */
    @NotNull
    private Long itemId;

    /** 产品。空表示通用。 */
    private Long productId;

    /** 规格下限。 */
    private BigDecimal lsl;

    /** 规格上限。 */
    private BigDecimal usl;

    /** 1 表示超规格自动 Hold。 */
    private Integer autoHoldOnOos;

    /** 自动 Hold 原因码。打开自动 Hold 时必填。 */
    private String holdReasonCode;

    /** 0 表示不必采。空按必采。 */
    private Integer isRequired;
}
