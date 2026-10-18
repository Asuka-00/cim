package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * BatchTrackInRequest 批次进站
 * <p>
 * 同一事务里让每个仍在批内的 Lot 进站。任一 Lot 失败则整批回滚。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class BatchTrackInRequest {

    /** 设备。 */
    @NotNull
    private Long equipmentId;

    /** Recipe 版本。 */
    private Long recipeVersionId;
}
