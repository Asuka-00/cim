package com.scaffold.mes.warehouse.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * InspectionRequest 来料检验
 * <p>
 * PASS 和 CONCESSION 变为可用，FAIL 进入隔离。特采必须写说明。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class InspectionRequest {

    /** PASS、FAIL 或 CONCESSION。 */
    @NotBlank
    private String result;

    /** 备注。特采时必填。 */
    private String remark;
}
