package com.scaffold.mes.core.domain.dto.notify;

/**
 * NotifyRuleResponse 一条已启用的通知规则
 *
 * @param id 规则
 * @param ruleCode 规则编码
 * @param ruleName 规则名称
 * @param eventType 事件类型
 * @param level 预警级别
 * @param titleTemplate 标题模板。空则用规则名称
 * @param contentTemplate 内容模板。空则预警内容为空
 * @param receiverType 接收范围 ROLE、USER、AREA 或 ALL
 * @param receiverId 接收对象。ALL 时为空
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record NotifyRuleResponse(Long id, String ruleCode, String ruleName, String eventType, String level,
        String titleTemplate, String contentTemplate, String receiverType, String receiverId) {
}
