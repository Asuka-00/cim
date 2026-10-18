package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * EcnApproveRequest 批准变更申请
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EcnApproveRequest {

    /** 审批人。 */
    @NotBlank
    private String approver;
}
