package com.scaffold.system.infra.dispatch.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.scaffold.share.integration.system.response.TenantView;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.repository.system.TenantRepository;

class SystemTenantProviderTest {

    @Test
    void findAllTenantsMapsTenantIdsToStrings() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        when(tenantRepository.findAll()).thenReturn(Arrays.asList(
                tenant(1L),
                tenant(2L)));
        SystemTenantProvider provider = new SystemTenantProvider(tenantRepository);

        assertThat(provider.findAllTenants())
                .containsExactly(new TenantView("1"), new TenantView("2"));
    }

    @Test
    void findAllTenantsSkipsNullTenantAndNullId() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        when(tenantRepository.findAll()).thenReturn(Arrays.asList(
                null,
                tenant(null),
                tenant(3L)));
        SystemTenantProvider provider = new SystemTenantProvider(tenantRepository);

        assertThat(provider.findAllTenants())
                .containsExactly(new TenantView("3"));
    }

    private Tenant tenant(Long id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        return tenant;
    }
}
