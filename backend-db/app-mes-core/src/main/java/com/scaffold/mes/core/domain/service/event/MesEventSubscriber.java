package com.scaffold.mes.core.domain.service.event;

import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;

/**
 * MesEventSubscriber 领域事件订阅者
 * <p>
 * 一个编码对应一个消费者,失败只重试该消费者。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesEventSubscriber {

    /**
     * 订阅者编码。同一事件按该编码各投递一次。
     *
     * @return 订阅者编码
     */
    String code();

    /**
     * 处理一条事件。抛出运行时异常时只重试本订阅者。
     *
     * @param event 领域事件
     */
    void onEvent(MesDomainEvent event);
}
