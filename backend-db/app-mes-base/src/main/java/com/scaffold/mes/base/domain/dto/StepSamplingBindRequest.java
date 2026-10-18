package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * StepSamplingBindRequest 把抽检规则绑到量测站点
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StepSamplingBindRequest {

    /** 量测站点。 */
    @NotNull
    private Long stepId;

    /** 限定产品。空表示不限产品。 */
    private Long productId;

    /** 抽检规则。 */
    @NotNull
    private Long ruleId;
}
