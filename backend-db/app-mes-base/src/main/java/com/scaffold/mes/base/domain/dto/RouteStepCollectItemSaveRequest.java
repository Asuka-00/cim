package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteStepCollectItemSaveRequest 保存 Route 步骤采集规格
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteStepCollectItemSaveRequest {

    /** Route 版本。 */
    @NotNull
    private Long routeVersionId;

    /** 步骤序号。 */
    @NotNull
    private Integer seqNo;

    /** 采集项。 */
    @NotNull
    private Long itemId;

    /** 限定产品。空表示通用。 */
    private Long productId;

    /** 规格下限。 */
    private BigDecimal lsl;

    /** 规格上限。 */
    private BigDecimal usl;

    /** 1 表示超规格自动 Hold。 */
    private Integer autoHoldOnOos;

    /** 自动 Hold 原因码。 */
    private String holdReasonCode;

    /** 0 表示不必采。空按必采。 */
    private Integer isRequired;
}
