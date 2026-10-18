package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteVersionDecisionRequest 审批或驳回 Route 版本
 * <p>
 * operator 必须是提交时指定的审批人。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteVersionDecisionRequest {

    /** 操作人，须与指定审批人一致。 */
    @NotBlank
    private String operator;

    /** 意见。 */
    private String comment;
}
