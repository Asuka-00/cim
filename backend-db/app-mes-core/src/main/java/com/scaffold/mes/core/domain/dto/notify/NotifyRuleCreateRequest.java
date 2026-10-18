package com.scaffold.mes.core.domain.dto.notify;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * NotifyRuleCreateRequest 新建通知规则
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class NotifyRuleCreateRequest {

    /** 规则编码。 */
    @NotBlank
    private String ruleCode;

    /** 规则名称。 */
    @NotBlank
    private String ruleName;

    /** 触发的领域事件类型。 */
    @NotBlank
    private String eventType;

    /** 标题模板。空则生成预警时用规则名称。 */
    private String titleTemplate;

    /** 内容模板。空则预警内容为空。 */
    private String contentTemplate;

    /** 预警级别 INFO、WARN 或 ERROR。空则 WARN。 */
    private String level;

    /** 接收范围 ROLE、USER、AREA 或 ALL。空则 ROLE。 */
    private String receiverType;

    /** 接收对象。范围不是 ALL 时必填。 */
    private String receiverId;
}
