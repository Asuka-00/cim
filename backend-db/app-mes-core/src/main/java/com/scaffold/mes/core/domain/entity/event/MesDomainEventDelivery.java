package com.scaffold.mes.core.domain.entity.event;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MesDomainEventDelivery 领域事件投递
 * <p>
 * 对应 mes_domain_event_delivery。每个订阅者独立重试。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_domain_event_delivery")
public class MesDomainEventDelivery extends NonSoftDeletableEntity {

    /** 领域事件ID */
    private Long eventId;

    /** 订阅者编码,如 WO_PROGRESS/ALERT_RULE/PM_COUNTER/MATERIAL_REQ */
    private String subscriber;

    /** 聚合类型(冗余,用于同聚合顺序投递) */
    private String aggregateType;

    /** 聚合ID(冗余) */
    private Long aggregateId;

    /** PENDING/DONE/FAILED/SKIPPED */
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
