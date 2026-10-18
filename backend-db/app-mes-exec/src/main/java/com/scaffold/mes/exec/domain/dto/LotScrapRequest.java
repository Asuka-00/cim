package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotScrapRequest 整批报废
 * <p>
 * 已绑定 Wafer 时报废全部存活片，数量由存活片数决定。没有片时必须填写报废数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotScrapRequest {

    /** MES_SCRAP_REASON 中的原因码。 */
    @NotBlank
    private String reasonCode;

    /** 不追踪个体时的报废数量。 */
    private Long scrapQty;

    /** 备注。 */
    private String remark;
}
