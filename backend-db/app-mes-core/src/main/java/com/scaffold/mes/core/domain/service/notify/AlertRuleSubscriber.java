package com.scaffold.mes.core.domain.service.notify;

import org.springframework.stereotype.Component;

import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.mes.core.domain.service.event.MesEventSubscriber;

import lombok.RequiredArgsConstructor;

/**
 * AlertRuleSubscriber 按通知规则生成预警
 * <p>
 * 同一事件只生成一条。停用的规则不会命中。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class AlertRuleSubscriber implements MesEventSubscriber {

    private final NotifyRuleApplicationService rules;

    @Override
    public String code() {
        return "ALERT_RULE";
    }

    @Override
    public void onEvent(MesDomainEvent event) {
        rules.raiseForEvent(event.getId(), event.getEventType());
    }
}
