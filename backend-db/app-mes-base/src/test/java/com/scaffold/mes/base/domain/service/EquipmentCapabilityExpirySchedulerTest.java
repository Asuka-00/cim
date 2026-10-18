package com.scaffold.mes.base.domain.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.mes.core.domain.service.event.TenantAwareTask;

/**
 * 能力过期任务在 MES 开启时按租户调用过期，关闭时不调用。
 */
class EquipmentCapabilityExpirySchedulerTest {

    @Test
    void expireRunsPerTenantWhenEnabled() {
        MesProperties properties = new MesProperties();
        TenantAwareTask tenantAwareTask = mock(TenantAwareTask.class);
        EquipmentCapabilityApplicationService service = mock(EquipmentCapabilityApplicationService.class);
        when(service.expireDue()).thenReturn(2);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(tenantAwareTask).run(any());
        EquipmentCapabilityExpiryScheduler scheduler = new EquipmentCapabilityExpiryScheduler(properties, tenantAwareTask, service);

        scheduler.expire();

        verify(service).expireDue();
    }

    @Test
    void expireSkipsWhenMesDisabled() {
        MesProperties properties = new MesProperties();
        properties.setEnabled(false);
        EquipmentCapabilityApplicationService service = mock(EquipmentCapabilityApplicationService.class);
        EquipmentCapabilityExpiryScheduler scheduler = new EquipmentCapabilityExpiryScheduler(properties, mock(TenantAwareTask.class), service);

        scheduler.expire();

        verify(service, never()).expireDue();
    }
}
