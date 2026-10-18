package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * PackageInputRequest 封装投入
 * <p>
 * 从源 Lot 扣数量，加到已经绑定工单的封装 Lot。扣完时源 Lot 变为 CONSUMED。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class PackageInputRequest {

    /** 源 Lot。 */
    @NotNull
    private Long sourceLotId;

    /** 投入数量。 */
    @NotNull
    private Long qty;
}
