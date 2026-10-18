package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * WaferScrapRequest 单片报废
 * <p>
 * 报废后 Lot 数量等于剩余存活片数。最后一片报废时 Lot 变为 SCRAPPED。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WaferScrapRequest {

    /** MES_SCRAP_REASON 中的原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
