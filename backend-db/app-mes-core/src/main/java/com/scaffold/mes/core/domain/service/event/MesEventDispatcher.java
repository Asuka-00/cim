package com.scaffold.mes.core.domain.service.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.mes.core.domain.entity.event.MesDomainEventDelivery;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventDeliveryRepository;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventRepository;
import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.share.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * MesEventDispatcher 领域事件分发器
 * <p>
 * 把新事件拆成按订阅者的投递,并按聚合顺序处理。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@Slf4j
public class MesEventDispatcher {

    private static final int MAX_RETRY = 5;

    private final MesProperties properties;
    private final MesDomainEventRepository eventRepository;
    private final MesDomainEventDeliveryRepository deliveryRepository;
    private final TenantAwareTask tenantAwareTask;
    private final Map<String, MesEventSubscriber> subscribers;
    private final TransactionTemplate tx;

    public MesEventDispatcher(MesProperties properties, MesDomainEventRepository eventRepository,
            MesDomainEventDeliveryRepository deliveryRepository, TenantAwareTask tenantAwareTask,
            List<MesEventSubscriber> subscribers, PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.eventRepository = eventRepository;
        this.deliveryRepository = deliveryRepository;
        this.tenantAwareTask = tenantAwareTask;
        this.subscribers = subscribers.stream()
                .collect(Collectors.toMap(MesEventSubscriber::code, Function.identity()));
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * 轮询新事件并处理到期投递。未启用 MES 时直接返回。
     */
    @Scheduled(fixedDelayString = "${app.mes.event-poll-ms:2000}")
    @SchedulerLock(name = "mesEventDispatch", lockAtMostFor = "PT1M", lockAtLeastFor = "PT1S")
    public void poll() {
        if (!properties.isEnabled()) {
            return;
        }
        tenantAwareTask.run(this::pollTenant);
    }

    private void pollTenant() {
        for (MesDomainEvent event : eventRepository.listNew(50)) {
            tx.executeWithoutResult(status -> openDeliveries(event));
        }
        for (MesDomainEventDelivery delivery : deliveryRepository.listDue(100)) {
            if (deliveryRepository.hasEarlierPending(delivery)) {
                continue;
            }
            tx.executeWithoutResult(status -> deliver(delivery));
        }
    }

    private void openDeliveries(MesDomainEvent event) {
        for (String code : subscribers.keySet()) {
            MesDomainEventDelivery delivery = new MesDomainEventDelivery();
            delivery.setEventId(event.getId());
            delivery.setSubscriber(code);
            delivery.setAggregateType(event.getAggregateType());
            delivery.setAggregateId(event.getAggregateId());
            delivery.setStatus("PENDING");
            delivery.setRetryCount(0);
            try {
                deliveryRepository.save(delivery);
            } catch (DuplicateKeyException ex) {
                // 重复分发
            }
        }
        event.setStatus("DISPATCHED");
        eventRepository.update(event);
    }

    private void deliver(MesDomainEventDelivery delivery) {
        MesEventSubscriber subscriber = subscribers.get(delivery.getSubscriber());
        MesDomainEvent event = eventRepository.findById(delivery.getEventId()).orElse(null);
        try {
            if (subscriber == null || event == null) {
                throw new IllegalStateException(subscriber == null ? "订阅者不存在" : "事件不存在");
            }
            subscriber.onEvent(event);
            delivery.setStatus("DONE");
            delivery.setProcessedAt(LocalDateTime.now());
            delivery.setErrorMessage(null);
        } catch (RuntimeException ex) {
            int retries = delivery.getRetryCount() == null ? 1 : delivery.getRetryCount() + 1;
            delivery.setRetryCount(retries);
            delivery.setErrorMessage(ex.getMessage());
            if (retries >= MAX_RETRY) {
                delivery.setStatus("FAILED");
                log.warn("事件投递放弃: eventId={}, subscriber={}", delivery.getEventId(), delivery.getSubscriber());
            } else {
                delivery.setNextRetryAt(LocalDateTime.now().plusSeconds(30));
            }
        }
        deliveryRepository.update(delivery);
    }

    /**
     * 把投递重新置为待处理。
     *
     * @param deliveryId 投递主键
     * @throws BusinessException 投递不存在
     */
    public void retry(Long deliveryId) {
        MesDomainEventDelivery delivery = require(deliveryId);
        delivery.setStatus("PENDING");
        delivery.setNextRetryAt(null);
        delivery.setRetryCount(0);
        deliveryRepository.update(delivery);
    }

    /**
     * 跳过投递,不再重试。
     *
     * @param deliveryId 投递主键
     * @throws BusinessException 投递不存在
     */
    public void skip(Long deliveryId) {
        MesDomainEventDelivery delivery = require(deliveryId);
        delivery.setStatus("SKIPPED");
        delivery.setProcessedAt(LocalDateTime.now());
        deliveryRepository.update(delivery);
    }

    private MesDomainEventDelivery require(Long id) {
        return deliveryRepository.findById(id).orElseThrow(() -> new BusinessException(MesErrorCodes.TXN_NOT_FOUND, "投递不存在"));
    }
}
