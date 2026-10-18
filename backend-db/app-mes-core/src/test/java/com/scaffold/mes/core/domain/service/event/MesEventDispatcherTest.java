package com.scaffold.mes.core.domain.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.scaffold.mes.core.config.MesProperties;
import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.mes.core.domain.entity.event.MesDomainEventDelivery;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventDeliveryRepository;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventRepository;
import com.scaffold.share.integration.system.provider.TenantProvider;

/**
 * 一个订阅者失败时,另一个订阅者保持成功,重试不会再调用它。
 */
class MesEventDispatcherTest {

    @Test
    void failedSubscriberDoesNotReplayTheSuccessfulOne() {
        MesDomainEvent event = new MesDomainEvent();
        event.setId(1L);
        event.setAggregateType("LOT");
        event.setAggregateId(3L);
        event.setStatus("NEW");

        List<MesDomainEventDelivery> deliveries = new ArrayList<>();
        MesDomainEventRepository events = mock(MesDomainEventRepository.class);
        when(events.listNew(50)).thenReturn(List.of(event), List.of());
        when(events.findById(1L)).thenReturn(Optional.of(event));

        MesDomainEventDeliveryRepository deliveryRepository = mock(MesDomainEventDeliveryRepository.class);
        doAnswer(invocation -> {
            deliveries.add(invocation.getArgument(0));
            return null;
        }).when(deliveryRepository).save(any());
        when(deliveryRepository.listDue(100)).thenAnswer(invocation -> deliveries.stream()
                .filter(delivery -> "PENDING".equals(delivery.getStatus()))
                .toList());
        when(deliveryRepository.hasEarlierPending(any())).thenReturn(false);

        AtomicInteger okRuns = new AtomicInteger();
        AtomicInteger badRuns = new AtomicInteger();
        MesEventSubscriber ok = subscriber("OK", okRuns, false);
        MesEventSubscriber bad = subscriber("BAD", badRuns, true);

        MesProperties properties = new MesProperties();
        properties.setEnabled(true);
        TenantAwareTask sameTenant = new TenantAwareTask(mock(TenantProvider.class)) {
            @Override
            public void run(Runnable task) {
                task.run();
            }
        };
        MesEventDispatcher dispatcher = new MesEventDispatcher(properties, events, deliveryRepository, sameTenant,
                List.of(ok, bad), immediate());

        dispatcher.poll();
        dispatcher.poll();

        assertThat(okRuns).hasValue(1);
        assertThat(badRuns).hasValue(2);
        assertThat(deliveries).filteredOn(delivery -> "OK".equals(delivery.getSubscriber()))
                .singleElement()
                .extracting(MesDomainEventDelivery::getStatus)
                .isEqualTo("DONE");
        assertThat(deliveries).filteredOn(delivery -> "BAD".equals(delivery.getSubscriber()))
                .singleElement()
                .extracting(MesDomainEventDelivery::getRetryCount)
                .isEqualTo(2);
    }

    @Test
    void laterEventWaitsUntilEarlierDeliveryOnSameLotIsDone() {
        MesDomainEvent trackOut = event(10L);
        MesDomainEvent reversed = event(11L);
        MesDomainEventDelivery first = delivery(1L, 10L);
        MesDomainEventDelivery second = delivery(2L, 11L);
        List<MesDomainEventDelivery> deliveries = new ArrayList<>(List.of(first, second));

        MesDomainEventRepository events = mock(MesDomainEventRepository.class);
        when(events.listNew(50)).thenReturn(List.of());
        when(events.findById(10L)).thenReturn(Optional.of(trackOut));
        when(events.findById(11L)).thenReturn(Optional.of(reversed));

        MesDomainEventDeliveryRepository deliveryRepository = mock(MesDomainEventDeliveryRepository.class);
        when(deliveryRepository.listDue(100)).thenAnswer(invocation -> deliveries.stream()
                .filter(delivery -> "PENDING".equals(delivery.getStatus()))
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .toList());
        when(deliveryRepository.hasEarlierPending(any())).thenAnswer(invocation -> {
            MesDomainEventDelivery current = invocation.getArgument(0);
            return deliveries.stream().anyMatch(delivery -> "PENDING".equals(delivery.getStatus())
                    && delivery.getSubscriber().equals(current.getSubscriber())
                    && delivery.getAggregateType().equals(current.getAggregateType())
                    && java.util.Objects.equals(delivery.getAggregateId(), current.getAggregateId())
                    && delivery.getId() < current.getId());
        });

        List<Long> handled = new ArrayList<>();
        MesEventSubscriber subscriber = new MesEventSubscriber() {
            @Override
            public String code() {
                return "PM_COUNTER";
            }

            @Override
            public void onEvent(MesDomainEvent event) {
                handled.add(event.getId());
                if (event.getId() == 10L) {
                    throw new IllegalStateException("出站处理失败");
                }
            }
        };

        MesEventDispatcher dispatcher = dispatcher(events, deliveryRepository, List.of(subscriber));
        dispatcher.poll();
        dispatcher.poll();

        assertThat(handled).containsExactly(10L, 10L);
        assertThat(second.getStatus()).isEqualTo("PENDING");
    }

    private static MesDomainEvent event(Long id) {
        MesDomainEvent event = new MesDomainEvent();
        event.setId(id);
        event.setAggregateType("LOT");
        event.setAggregateId(3L);
        event.setStatus("DISPATCHED");
        return event;
    }

    private static MesDomainEventDelivery delivery(Long id, Long eventId) {
        MesDomainEventDelivery delivery = new MesDomainEventDelivery();
        delivery.setId(id);
        delivery.setEventId(eventId);
        delivery.setSubscriber("PM_COUNTER");
        delivery.setAggregateType("LOT");
        delivery.setAggregateId(3L);
        delivery.setStatus("PENDING");
        delivery.setRetryCount(0);
        return delivery;
    }

    private static MesEventDispatcher dispatcher(MesDomainEventRepository events,
            MesDomainEventDeliveryRepository deliveryRepository, List<MesEventSubscriber> subscribers) {
        MesProperties properties = new MesProperties();
        properties.setEnabled(true);
        TenantAwareTask sameTenant = new TenantAwareTask(mock(TenantProvider.class)) {
            @Override
            public void run(Runnable task) {
                task.run();
            }
        };
        return new MesEventDispatcher(properties, events, deliveryRepository, sameTenant, subscribers, immediate());
    }

    private static MesEventSubscriber subscriber(String code, AtomicInteger runs, boolean fail) {
        return new MesEventSubscriber() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public void onEvent(MesDomainEvent event) {
                runs.incrementAndGet();
                if (fail) {
                    throw new IllegalStateException(code);
                }
            }
        };
    }

    private static PlatformTransactionManager immediate() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
