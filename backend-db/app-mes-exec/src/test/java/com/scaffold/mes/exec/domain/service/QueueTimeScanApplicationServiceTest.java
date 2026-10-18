package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.integration.mes.base.provider.QueueTimeProvider;
import com.scaffold.share.integration.mes.base.response.QueueTimeLimit;

/**
 * 未进站且超过排队上限时自动 Hold。WARN 和未超时不扣押。
 */
class QueueTimeScanApplicationServiceTest {

    private LotRepository lotRepository;
    private QueueTimeProvider queueTimes;
    private LotHoldApplicationService holds;
    private QueueTimeScanApplicationService service;
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 16, 15, 0);

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        queueTimes = mock(QueueTimeProvider.class);
        holds = mock(LotHoldApplicationService.class);
        service = new QueueTimeScanApplicationService(lotRepository, queueTimes, holds);
    }

    @Test
    void overdueWaitingLotIsHeld() {
        when(lotRepository.listWaiting(null, null)).thenReturn(List.of(lot(14, 0)));
        when(queueTimes.limit(21L, 10)).thenReturn(new QueueTimeLimit(30, "HOLD"));

        assertThat(service.scan(now)).isEqualTo(1);
        verify(holds).holdSystem(8L, "QUEUE_TIME", "QUEUE_TIME", 7L);
    }

    @Test
    void warnAndFreshLotsStayOpen() {
        Lot warned = lot(14, 0);
        warned.setId(9L);
        when(lotRepository.listWaiting(null, null)).thenReturn(List.of(warned, lot(14, 50)));
        when(queueTimes.limit(21L, 10)).thenReturn(new QueueTimeLimit(30, "WARN"));

        assertThat(service.scan(now)).isZero();
        verify(holds, never()).holdSystem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static Lot lot(int hour, int minute) {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setStepId(7L);
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        lot.setStepEnterAt(LocalDateTime.of(2026, 10, 16, hour, minute));
        return lot;
    }
}
