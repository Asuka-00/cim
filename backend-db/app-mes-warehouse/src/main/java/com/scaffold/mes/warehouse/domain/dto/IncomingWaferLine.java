package com.scaffold.mes.warehouse.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * IncomingWaferLine 一条来料 Wafer
 * <p>
 * 编号在同一物料批内不能重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class IncomingWaferLine {

    /** 来料 Wafer 编号。 */
    @NotBlank
    private String vendorWaferId;

    /** 来料槽位，可空。 */
    private Integer slotNo;
}
