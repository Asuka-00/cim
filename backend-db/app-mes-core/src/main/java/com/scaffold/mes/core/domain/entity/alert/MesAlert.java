package com.scaffold.mes.core.domain.entity.alert;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MesAlert 站内预警
 * <p>
 * 对应 mes_alert。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_alert")
public class MesAlert extends NonSoftDeletableEntity {

    /** MATERIAL_LOW/MATERIAL_EXPIRED/BINDING_DEPLETED/QUEUE_TIME/TIME_LINK/HOLD_TIMEOUT/TOOL_LIFE/EQP_FAULT/PM_DUE/SYSTEM */
    private String alertType;

    /** INFO/WARN/ERROR */
    private String level;

    /** 对象类型 */
    private String subjectType;

    /** 对象ID */
    private Long subjectId;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** NEW/READ/CLOSED */
    private String status;

    /** 接收范围 ALL/ROLE:xxx/USER:xxx/AREA:xxx */
    private String receiverScope;

    /** 已读人 */
    private String readBy;

    /** 已读时间 */
    private LocalDateTime readAt;

    /** 来源领域事件 */
    private Long eventId;

    /** 来源通知规则编码。 */
    private String ruleCode;

}
