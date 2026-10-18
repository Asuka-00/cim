package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotTimeLinkTimer 时间链计时
 * <p>
 * 对应 mes_lot_time_link_timer。起点出站时开 ACTIVE，终点进站时再关掉。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot_time_link_timer")
public class LotTimeLinkTimer extends NonSoftDeletableEntity {

    /** Lot。 */
    private Long lotId;

    /** 时间链约束。 */
    private Long timeLinkId;

    /** Route 版本。 */
    private Long routeVersionId;

    /** 起点序号。 */
    private Integer fromSeqNo;

    /** 终点序号。 */
    private Integer toSeqNo;

    /** 开始时间。 */
    private LocalDateTime startAt;

    /** 超限时间。 */
    private LocalDateTime deadlineAt;

    /** ACTIVE、DONE 或 CANCELLED。 */
    private String status;

    /** NONE、WARNED 或 BREACHED。 */
    private String alertLevel;

    /** HOLD 或 WARN。 */
    private String action;

    /** 自动 Hold 原因码。 */
    private String reasonCode;

    /** 开始计时的事务。 */
    private Long startTxnId;
}
