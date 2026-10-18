package com.scaffold.mes.exec.domain.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * WipImportRequest 导入一条期初在制
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WipImportRequest {

    /** Lot 号。 */
    @NotBlank
    private String lotNo;

    /** 数量，必须大于 0。 */
    @NotNull
    private Long qty;

    /** 当前步骤序号。 */
    @NotNull
    private Integer seqNo;

    /** 当前站点。 */
    @NotNull
    private Long stepId;

    /** 当前站点已经过的次数。空则 0。到达序号是这个数加 1。 */
    private Integer passCount;

    /** 一个已经过的站点。和 priorPassCount 一起填。 */
    private Long priorStepId;

    /** 已经过站点的经过次数，至少 1。 */
    private Integer priorPassCount;

    /** 尚未释放的 Hold 原因码。空则不 Hold。 */
    private String holdReasonCode;

    /** 进入当前站点的时间。空则用当前时间。 */
    private LocalDateTime stepEnterAt;

    /** 上次出站时间。空则不填。 */
    private LocalDateTime lastTrackOutAt;

    /** 返工单。和 reworkSeqNo 一起填。 */
    private Long reworkId;

    /** 返工步骤序号，至少 1。 */
    private Integer reworkSeqNo;

    /** 所在库位。空则不填。 */
    private Long locationId;

    /** 覆盖的 Recipe 版本。空则不覆盖。 */
    private Long recipeVersionId;

    /** 尚未结束的时间链。和 timeLinkStartAt 一起填。 */
    private Long timeLinkId;

    /** 时间链计时起点。 */
    private LocalDateTime timeLinkStartAt;

    /** 在制 Wafer 编号。和 slotNo 一起填。 */
    private String waferCode;

    /** Wafer 槽位，至少 1。同一 Lot 不能重复。 */
    private Integer slotNo;

    /** Wafer 状态。空则 NORMAL。也可以是 SCRAPPED。 */
    private String waferStatus;

    /** 报废原因码。状态为 SCRAPPED 时必填。 */
    private String scrapReason;
}
