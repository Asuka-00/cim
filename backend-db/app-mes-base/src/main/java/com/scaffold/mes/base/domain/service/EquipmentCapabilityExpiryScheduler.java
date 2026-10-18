package com.scaffold.mes.base.domain.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.mes.core.domain.service.event.TenantAwareTask;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * EquipmentCapabilityExpiryScheduler 设备能力过期任务
 * <p>
 * 每天把已过有效期的 VALID 记录改成 EXPIRED。多实例由调度锁保证只跑一次。MES 关闭时不执行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EquipmentCapabilityExpiryScheduler {

    private final MesProperties properties;
    private final TenantAwareTask tenantAwareTask;
    private final EquipmentCapabilityApplicationService service;

    /**
     * 按租户执行过期。默认每天 00:10（上海时区）。
     */
    @Scheduled(cron = "${app.mes.capability-expire-cron:0 10 0 * * *}", zone = "Asia/Shanghai")
    @SchedulerLock(name = "mesCapabilityExpire", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void expire() {
        if (!properties.isEnabled()) {
            return;
        }
        tenantAwareTask.run(() -> {
            int count = service.expireDue();
            if (count > 0) {
                log.info("过期设备能力 {} 条", count);
            }
        });
    }
}
