package com.scaffold.mes.core.domain.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.scaffold.share.context.TenantContextHolder;
import com.scaffold.share.integration.system.provider.TenantProvider;
import com.scaffold.share.integration.system.response.TenantView;

/**
 * 后台任务必须逐个租户执行，结束后恢复原来的租户。
 */
class TenantAwareTaskTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void runsOncePerTenantAndRestoresPreviousTenant() {
        TenantProvider provider = mock(TenantProvider.class);
        when(provider.findAllTenants()).thenReturn(List.of(new TenantView("A"), new TenantView("B")));
        TenantContextHolder.setTenantId("original");
        List<String> seen = new ArrayList<>();

        new TenantAwareTask(provider).run(() -> seen.add(TenantContextHolder.getTenantId()));

        assertThat(seen).containsExactly("A", "B");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("original");
    }
}
