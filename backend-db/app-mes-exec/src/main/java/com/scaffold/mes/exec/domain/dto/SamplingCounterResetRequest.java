package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * SamplingCounterResetRequest 手工清零抽检计数
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class SamplingCounterResetRequest {

    /** 规则。 */
    @NotNull
    private Long ruleId;

    /** 站点。 */
    @NotNull
    private Long stepId;

    /** Route 步骤。空按不区分步骤。 */
    private Long routeStepId;

    /** 产品。空按通用。 */
    private Long productId;
}
