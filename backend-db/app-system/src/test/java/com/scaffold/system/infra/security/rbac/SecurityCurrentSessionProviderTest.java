package com.scaffold.system.infra.security.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.scaffold.share.context.SessionPrincipal;
import com.scaffold.share.context.TenantContextHolder;

/**
 * 显式租户必须优先于登录主体。
 */
class SecurityCurrentSessionProviderTest {

    private final SecurityCurrentSessionProvider provider = new SecurityCurrentSessionProvider();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void explicitTenantWinsOverLoginPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new SessionPrincipal("operator", "tenant-a"), null));
        TenantContextHolder.setTenantId("tenant-b");

        assertThat(provider.getCurrentTenantId()).isEqualTo("tenant-b");
    }

    @Test
    void fallsBackToLoginPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new SessionPrincipal("operator", "tenant-a"), null));

        assertThat(provider.getCurrentTenantId()).isEqualTo("tenant-a");
    }
}
