package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ImportWipRowRequest 给导入任务写一行在制
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ImportWipRowRequest {

    /** 工单。 */
    @NotNull
    private Long woId;

    /** 这一行在制。 */
    @NotNull
    @Valid
    private WipImportRequest row;

    /** 行号，从 1 开始。 */
    @NotNull
    private Integer rowNo;
}
