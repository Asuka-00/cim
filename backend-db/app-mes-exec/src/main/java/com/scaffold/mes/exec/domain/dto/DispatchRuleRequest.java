package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * DispatchRuleRequest 新建排序规则
 * <p>
 * 排序键用逗号分隔。认识 IS_HOT、PRIORITY、STEP_ENTER_AT。不认识的键会跳过。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class DispatchRuleRequest {

    /** 规则编码。 */
    @NotBlank
    private String ruleCode;

    /** 规则名称。 */
    @NotBlank
    private String ruleName;

    /** 排序键。 */
    @NotBlank
    private String sortKeys;

    /** 1 表示默认规则。 */
    private Integer isDefault;
}
