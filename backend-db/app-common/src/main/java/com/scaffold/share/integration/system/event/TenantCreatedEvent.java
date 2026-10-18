package com.scaffold.share.integration.system.event;

/**
 * TenantCreatedEvent 租户已创建事件
 * <p>
 * 新建租户提交后发布。消费方按租户做幂等初始化。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record TenantCreatedEvent(String tenantId) {
}
