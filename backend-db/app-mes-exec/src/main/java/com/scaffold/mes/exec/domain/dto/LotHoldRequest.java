package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotHoldRequest 手工 Hold
 * <p>
 * 同一原因可以多次 Hold。不写幂等键。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotHoldRequest {

    /** 原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
