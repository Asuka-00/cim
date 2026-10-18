package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotReworkRequest 进入返工
 * <p>
 * 当前主流程序号必须落在分支允许的范围内。次数按根 Lot 的历史计算。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotReworkRequest {

    /** 返工分支。 */
    @NotNull
    private Long reworkId;

    /** 原因码。 */
    private String reasonCode;
}
