package com.scaffold.system.infra.dispatch.system;

import java.util.List;

import org.springframework.stereotype.Component;

import com.scaffold.share.integration.system.provider.TenantProvider;
import com.scaffold.share.integration.system.response.TenantView;
import com.scaffold.system.domain.repository.system.TenantRepository;

import lombok.RequiredArgsConstructor;

/**
 * Adapts system tenant repository to tenant lookup contract for dispatch.
 */
@Component
@RequiredArgsConstructor
public class SystemTenantProvider implements TenantProvider {

    private final TenantRepository tenantRepository;

    @Override
    public List<TenantView> findAllTenants() {
        return tenantRepository.findAll().stream()
                .filter(tenant -> tenant != null && tenant.getId() != null)
                .map(tenant -> new TenantView(String.valueOf(tenant.getId())))
                .toList();
    }
}
