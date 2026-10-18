package com.scaffold.mes.exec.domain.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.Setter;

/**
 * WaferTrackRequest 片级进出站
 * <p>
 * 只处理列出的片。没列出的片留在原步骤，并拆成子 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WaferTrackRequest {

    /** 要进站或出站的片。 */
    @NotEmpty
    private List<Long> waferIds;

    /** 进站站点。出站时忽略。 */
    private Long stepId;

    /** 进站设备。出站时忽略。 */
    private Long equipmentId;

    /** 进站 Recipe 版本。出站时忽略。 */
    private Long recipeVersionId;
}
