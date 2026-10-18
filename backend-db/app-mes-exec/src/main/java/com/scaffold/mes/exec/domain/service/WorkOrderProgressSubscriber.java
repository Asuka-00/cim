package com.scaffold.mes.exec.domain.service;

import org.springframework.stereotype.Component;

import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.mes.core.domain.service.event.MesEventSubscriber;

import lombok.RequiredArgsConstructor;

/**
 * WorkOrderProgressSubscriber 工单进度订阅
 * <p>
 * 收到 WORK_ORDER_PROGRESS 后按 Lot 重算完成、报废和终止数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class WorkOrderProgressSubscriber implements MesEventSubscriber {

    private final WorkOrderProgressApplicationService progress;

    @Override
    public String code() {
        return "WORK_ORDER_PROGRESS";
    }

    @Override
    public void onEvent(MesDomainEvent event) {
        if (event.getAggregateId() != null && "WORK_ORDER_PROGRESS".equals(event.getEventType())) {
            progress.refresh(event.getAggregateId());
        }
    }
}
