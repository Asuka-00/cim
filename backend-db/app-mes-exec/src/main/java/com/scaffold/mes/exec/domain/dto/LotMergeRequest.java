package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LotMergeRequest 合批
 * <p>
 * 源 Lot 并入路径上的目标 Lot。源 Lot 随后变为 MERGED，数量清零。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotMergeRequest {

    /** 被并入的源 Lot。 */
    @NotNull
    private Long sourceLotId;
}
