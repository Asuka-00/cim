package com.scaffold.mes.core.domain.service.event;

import org.springframework.stereotype.Component;

import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.share.integration.system.provider.TenantProvider;

import lombok.RequiredArgsConstructor;

/**
 * 无登录态的后台任务按租户执行。依赖 {@code runAs} 优先于登录主体。
 */
@Component
@RequiredArgsConstructor
public class TenantAwareTask {

    private final TenantProvider tenantProvider;

    /**
     * 对每个租户执行任务,并在结束后恢复原来的租户上下文。
     *
     * @param task 单个租户内要执行的动作
     */
    public void run(Runnable task) {
        for (var tenant : tenantProvider.findAllTenants()) {
            TenantContextHolder.runAs(tenant.tenantId(), task);
        }
    }
}
