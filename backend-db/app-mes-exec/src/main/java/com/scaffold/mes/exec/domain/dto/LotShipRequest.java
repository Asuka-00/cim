package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotShipRequest 出货
 * <p>
 * 整批出货。Lot 必须已经完成，并且没有 Hold。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotShipRequest {

    /** 出货单号。 */
    @NotBlank
    private String shipNo;

    /** 备注。 */
    private String remark;
}
