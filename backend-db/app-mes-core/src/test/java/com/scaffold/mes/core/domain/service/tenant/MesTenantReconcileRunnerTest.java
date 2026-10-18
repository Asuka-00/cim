package com.scaffold.mes.core.domain.service.tenant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.share.integration.system.provider.TenantProvider;
import com.scaffold.share.integration.system.response.TenantView;

/**
 * MES 开启时，启动会给每个已有租户补授权。关闭时不碰租户。
 */
class MesTenantReconcileRunnerTest {

    @Test
    void runSeedsExistingTenants() {
        MesProperties properties = new MesProperties();
        TenantProvider tenantProvider = mock(TenantProvider.class);
        MesTenantInitializer initializer = mock(MesTenantInitializer.class);
        when(tenantProvider.findAllTenants()).thenReturn(List.of(new TenantView("tenant-a"), new TenantView("tenant-b")));
        MesTenantReconcileRunner runner = new MesTenantReconcileRunner(properties, tenantProvider, initializer);

        runner.run(null);

        verify(initializer).init("tenant-a");
        verify(initializer).init("tenant-b");
    }

    @Test
    void runSkipsWhenMesDisabled() {
        MesProperties properties = new MesProperties();
        properties.setEnabled(false);
        TenantProvider tenantProvider = mock(TenantProvider.class);
        MesTenantInitializer initializer = mock(MesTenantInitializer.class);
        MesTenantReconcileRunner runner = new MesTenantReconcileRunner(properties, tenantProvider, initializer);

        runner.run(null);

        verify(tenantProvider, never()).findAllTenants();
        verify(initializer, never()).init(org.mockito.ArgumentMatchers.any());
    }
}
