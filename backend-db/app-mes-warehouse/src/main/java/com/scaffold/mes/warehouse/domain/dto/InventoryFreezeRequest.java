package com.scaffold.mes.warehouse.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * InventoryFreezeRequest 库位部分冻结
 * <p>
 * 只增加该库位的冻结数量，不改变在库数量。可用量是在库减去冻结。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class InventoryFreezeRequest {

    /** 物料批。 */
    @NotNull
    private Long materialLotId;

    /** 库位。 */
    @NotNull
    private Long locationId;

    /** 本次冻结数量。 */
    @NotNull
    private BigDecimal qty;
}
