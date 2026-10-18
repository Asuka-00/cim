package com.scaffold.mes.exec.domain.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ConsumptionAdjustRequest 消耗修正
 * <p>
 * 填写修正后的数量。差额追加，原记录不改。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ConsumptionAdjustRequest {

    /** 修正后的数量。 */
    @NotNull
    private BigDecimal qtyAfter;
}
