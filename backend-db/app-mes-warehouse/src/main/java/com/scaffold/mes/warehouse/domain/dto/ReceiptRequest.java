package com.scaffold.mes.warehouse.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ReceiptRequest 来料收货
 * <p>
 * 一次确认生成物料批并入库。needInspect 为 true 时物料批停在待检。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ReceiptRequest {

    /** 收货单号。 */
    @NotBlank
    private String receiptNo;

    /** 物料批号。 */
    @NotBlank
    private String lotCode;

    /** 物料。 */
    @NotNull
    private Long materialId;

    /** 库位。 */
    @NotNull
    private Long locationId;

    /** 计量单位。 */
    @NotNull
    private Long uomId;

    /** 数量。 */
    @NotNull
    private BigDecimal qty;

    /** 是否需要来料检验。 */
    private boolean needInspect;
}
