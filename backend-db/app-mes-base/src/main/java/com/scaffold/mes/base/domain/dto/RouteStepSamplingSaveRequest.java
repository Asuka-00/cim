package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteStepSamplingSaveRequest 保存 Route 步骤抽检覆盖
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteStepSamplingSaveRequest {

    /** Route 版本。 */
    @NotNull
    private Long routeVersionId;

    /** 步骤序号。 */
    @NotNull
    private Integer seqNo;

    /** 限定产品。空表示通用。 */
    private Long productId;

    /** 抽检规则。 */
    @NotNull
    private Long ruleId;
}
