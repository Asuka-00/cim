package com.scaffold.mes.core.domain.entity.event;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MesDomainEvent 内部领域事件
 * <p>
 * 对应 mes_domain_event。同事务写入,由调度任务按订阅者分发。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_domain_event")
public class MesDomainEvent extends NonSoftDeletableEntity {

    /** 事件号(雪花ID字符串) */
    private String eventNo;

    /** 事件类型,如 LOT_HOLD/LOT_TRACK_OUT/MATERIAL_LOW */
    private String eventType;

    /** 聚合类型 */
    private String aggregateType;

    /** 聚合ID */
    private Long aggregateId;

    /** 来源事务ID */
    private Long txnId;

    /** 事件内容 */
    private String payloadJson;

    /** 分发状态 NEW/DISPATCHED/FAILED(订阅者处理状态见投递表) */
    private String status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 下次重试时间 */
    private LocalDateTime nextRetryAt;

    /** 处理完成时间 */
    private LocalDateTime processedAt;

    /** 最近失败原因 */
    private String errorMessage;

}
