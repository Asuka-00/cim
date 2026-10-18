package com.scaffold.mes.core.domain.service.tenant;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.share.integration.system.provider.TenantProvider;

import lombok.RequiredArgsConstructor;

/**
 * MesTenantReconcileRunner 启动时补齐已有租户的 MES 授权
 * <p>
 * 新建租户会走初始化事件。已存在的租户在菜单增加后，要在下次启动时把缺的权限授给 default 或 MES_ADMIN。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class MesTenantReconcileRunner implements ApplicationRunner {

    private final MesProperties properties;
    private final TenantProvider tenantProvider;
    private final MesTenantInitializer initializer;

    /**
     * 应用启动后，对每个租户做一次幂等初始化。
     *
     * @param args 启动参数，不使用
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        for (var tenant : tenantProvider.findAllTenants()) {
            initializer.init(tenant.tenantId());
        }
    }
}
