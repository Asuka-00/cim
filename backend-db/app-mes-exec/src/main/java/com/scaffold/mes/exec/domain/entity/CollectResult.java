package com.scaffold.mes.exec.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * CollectResult 采集结果
 * <p>
 * 对应 hist_mes_collect_result。只追加。本阶段只记原始数，判定固定为 NA。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_collect_result")
public class CollectResult extends AppendOnlyEntity {

    /** Lot。 */
    private Long lotId;

    /** ORIGINAL、CORRECTION 或 VOID。 */
    private String recordType;

    /** 被作废的原记录。 */
    private Long refId;

    /** 产品。 */
    private Long productId;

    /** 步骤序号。 */
    private Integer routeSeqNo;

    /** 站点。 */
    private Long stepId;

    /** 第几次经过。 */
    private Integer passNo;

    /** 采集项。 */
    private Long itemId;

    /** 原始数值。 */
    private BigDecimal valueNum;

    /** OK、OOS_LOW、OOS_HIGH 或 NA。 */
    private String judgeResult;

    /** 判定时的下限。 */
    private BigDecimal lsl;

    /** 判定时的上限。 */
    private BigDecimal usl;

    /** 录入时间。 */
    private LocalDateTime collectedAt;
}
