package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * FutureAction 预约动作
 * <p>
 * 对应 mes_future_action。到达主流程步骤时执行仍为 PENDING 的 ON_ARRIVE。返工步骤不触发。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_future_action")
public class FutureAction extends NonSoftDeletableEntity {

    /** Lot。 */
    private Long lotId;

    /** 预约时的 Route 版本。 */
    private Long routeVersionId;

    /** 触发的主流程序号。 */
    private Integer routeSeqNo;

    /** 第几次经过。空表示每次到达都触发。 */
    private Integer passNo;

    /** 触发时机。 */
    private String timing;

    /** 动作类型。 */
    private String actionType;

    /** 动作参数。Hold 放原因码，改优先级放数字。 */
    private String actionParams;

    /** PENDING、DONE、FAILED 或 CANCELLED。 */
    private String status;

    /** 执行时间。 */
    private LocalDateTime executedAt;

    /** 执行结果。 */
    private String resultMessage;
}
