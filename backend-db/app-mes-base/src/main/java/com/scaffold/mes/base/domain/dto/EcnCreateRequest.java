package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * EcnCreateRequest 新建变更申请
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class EcnCreateRequest {

    /** 编号。 */
    @NotBlank
    private String ecnNo;

    /** 标题。 */
    @NotBlank
    private String title;

    /** ROUTE、RECIPE、BOM、PRODUCT 或 OTHER。 */
    @NotBlank
    private String changeType;

    /** 变更原因。 */
    @NotBlank
    private String reason;

    /** 申请人。 */
    private String applicant;
}
