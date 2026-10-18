package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotBonusRequest 补片
 * <p>
 * 增加在制 Lot 的开批数量。已有 Wafer 时按数量新建片。来料库存扣减在 M9。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotBonusRequest {

    /** 补入数量，必须大于 0。 */
    @NotNull
    private Long qty;
}
