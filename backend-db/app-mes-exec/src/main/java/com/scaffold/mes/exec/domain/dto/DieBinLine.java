package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * DieBinLine 一条 Bin 数量
 * <p>
 * 没有逐片明细时，用来录入 Lot 汇总。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class DieBinLine {

    /** Bin 编码。 */
    @NotBlank
    private String binCode;

    /** Bin 名称。 */
    private String binName;

    /** 是否良品。空表示不良。 */
    private Boolean good;

    /** Die 数量，不能为负。 */
    @NotNull
    private Long dieQty;
}
