package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * CollectVoidRequest 作废当前经过的最后一条采集值
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class CollectVoidRequest {

    /** Lot。 */
    @NotNull
    private Long lotId;

    /** 采集项。 */
    @NotNull
    private Long itemId;
}
