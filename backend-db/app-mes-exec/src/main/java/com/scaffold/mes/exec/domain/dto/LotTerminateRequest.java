package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotTerminateRequest 终止
 * <p>
 * 在制 Lot 变为 TERMINATED。数量保留，不并入报废。Hold 不阻止。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotTerminateRequest {

    /** 原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
