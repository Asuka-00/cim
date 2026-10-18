package com.scaffold.mes.core.domain.entity.notify;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * NotifyRule 通知规则
 * <p>
 * 对应 mes_notify_rule。新建时启用，级别为 WARN，接收范围是 ROLE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_notify_rule")
public class NotifyRule extends NonSoftDeletableEntity {

    /** 规则编码。 */
    private String ruleCode;

    /** 规则名称。 */
    private String ruleName;

    /** 触发的领域事件类型。 */
    private String eventType;

    /** 标题模板。空则生成预警时用规则名称。 */
    private String titleTemplate;

    /** 内容模板。空则预警内容为空。 */
    private String contentTemplate;

    /** INFO、WARN 或 ERROR。 */
    private String level;

    /** ROLE、USER、AREA 或 ALL。 */
    private String receiverType;

    /** 接收对象。ALL 时为空。 */
    private String receiverId;

    /** 1 表示启用。 */
    private Integer enabled;
}
