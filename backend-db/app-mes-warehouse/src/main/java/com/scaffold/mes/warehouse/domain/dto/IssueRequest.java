package com.scaffold.mes.warehouse.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * IssueRequest 领料
 * <p>
 * 从源库位扣到目标库位。只有可用物料批可以领。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class IssueRequest {

    /** 物料批。 */
    @NotNull
    private Long materialLotId;

    /** 源库位。 */
    @NotNull
    private Long fromLocationId;

    /** 目标库位。 */
    @NotNull
    private Long toLocationId;

    /** 数量。 */
    @NotNull
    private BigDecimal qty;
}
