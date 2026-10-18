package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotSamplingPlan Lot 抽检决定
 * <p>
 * 对应 mes_lot_sampling_plan。同一 Lot、步骤序号和经过次数只决定一次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot_sampling_plan")
public class LotSamplingPlan extends NonSoftDeletableEntity {

    /** Lot。 */
    private Long lotId;

    /** 量测步骤序号。 */
    private Integer routeSeqNo;

    /** 第几次经过。 */
    private Integer passNo;

    /** 量测站点。 */
    private Long stepId;

    /** 依据的规则。 */
    private Long ruleId;

    /** 1 表示需要量测。 */
    private Integer needMeasure;

    /** 抽中时要抽的片数。空表示全部或未抽中。 */
    private Integer waferCount;

    /** 抽中的槽位，逗号分隔。 */
    private String waferSlots;

    /** 决定时间。 */
    private LocalDateTime decidedAt;
}
