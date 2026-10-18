package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotStartRequest 开批请求
 * <p>
 * 数量计入工单计划余量。工艺版本从工单上带出，不由调用方指定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotStartRequest {

    /** Lot 号。 */
    @NotBlank
    private String lotNo;

    /** 类型。空则按 PRODUCTION。 */
    private String lotType;

    /** 开批数量，必须大于 0。 */
    @NotNull
    private Long qty;

    /** 备注。 */
    private String remark;
}
