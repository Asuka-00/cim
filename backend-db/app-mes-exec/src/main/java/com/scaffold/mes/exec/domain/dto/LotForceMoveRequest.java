package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotForceMoveRequest 强制跳站
 * <p>
 * 跳到当前 Route 上的任意步骤。被跳过的站点不增加经过次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotForceMoveRequest {

    /** 目标主流程序号。 */
    @NotNull
    private Integer routeSeqNo;

    /** 原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
