package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * TrackInRequest 进站请求
 * <p>
 * 站点必须是 Lot 当前步骤。不要求设备或 Recipe 的站点可以不填对应字段。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class TrackInRequest {

    /** 操作站点。 */
    @NotNull
    private Long stepId;

    /** 设备。 */
    private Long equipmentId;

    /** Port 或腔体。 */
    private Long portId;

    /** Recipe 版本。 */
    private Long recipeVersionId;

    /** 本次使用的工具。空表示不检查寿命。 */
    private Long toolId;
}
