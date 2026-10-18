package com.scaffold.mes.exec.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * CollectResultRequest 录入一条采集值
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class CollectResultRequest {

    /** Lot。 */
    @NotNull
    private Long lotId;

    /** 采集项。 */
    @NotNull
    private Long itemId;

    /** 原始数值。 */
    @NotNull
    private BigDecimal valueNum;
}
