package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * BatchCreateRequest 新建批次
 * <p>
 * 容量在创建时固定。站点上的批量配置还没落到站点表，先由调用方传入上下限。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class BatchCreateRequest {

    /** 批次号。 */
    @NotBlank
    private String batchNo;

    /** 站点。 */
    @NotNull
    private Long stepId;

    /** 最少 Lot 数。空表示 1。 */
    private Integer minLots;

    /** 最多 Lot 数，至少 1。 */
    @NotNull
    private Integer maxLots;
}
