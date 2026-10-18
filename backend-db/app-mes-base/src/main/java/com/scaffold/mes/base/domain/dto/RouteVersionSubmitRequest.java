package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * RouteVersionSubmitRequest 提交 Route 版本审批
 * <p>
 * 指定审批人。只有该审批人可以通过或驳回。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class RouteVersionSubmitRequest {

    /** 指定审批人。 */
    @NotBlank
    private String assignee;

    /** 意见。 */
    private String comment;
}
