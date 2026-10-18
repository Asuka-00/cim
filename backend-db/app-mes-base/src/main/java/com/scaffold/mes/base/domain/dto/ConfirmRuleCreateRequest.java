package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * ConfirmRuleCreateRequest 新建二次确认规则
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ConfirmRuleCreateRequest {

    /** 动作编码。 */
    @NotBlank
    private String actionCode;

    /** REAUTH 或 SECOND_SIGN。 */
    @NotBlank
    private String confirmMode;

    /** 第二签名人角色。SECOND_SIGN 时必填。 */
    private String secondRoleCode;
}
