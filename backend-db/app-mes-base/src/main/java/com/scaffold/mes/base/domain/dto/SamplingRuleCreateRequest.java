package com.scaffold.mes.base.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * SamplingRuleCreateRequest 新建按间隔批数的抽检规则
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class SamplingRuleCreateRequest {

    /** 规则编码。 */
    @NotBlank
    private String ruleCode;

    /** 规则名称。 */
    @NotBlank
    private String ruleName;

    /** 每 N 个 Lot 抽 1 个。EVERY_N_LOT 时必填。 */
    private Integer nValue;

    /** EVERY_N_LOT 或 PERCENT。空按 EVERY_N_LOT。 */
    private String sampleType;

    /** 抽检百分比。PERCENT 时必填。 */
    private BigDecimal percent;

    /** 每 Lot 抽取片数。空表示全部。 */
    private Integer waferCount;

    /** 指定槽位，逗号分隔。空表示不指定。 */
    private String slotList;
}
