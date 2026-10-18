package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotSkipRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

/**
 * 跳站只前进一个序号，不增加被跳过站点的经过次数。
 */
class LotSkipApplicationServiceTest {

    private LotStepPassApplicationService stepPass;
    private Lot lot;
    private LotSkipApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lotRepository = mock(LotRepository.class);
        stepPass = mock(LotStepPassApplicationService.class);
        RouteNextProvider routeNext = mock(RouteNextProvider.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setHoldCount(1);
        lot.setStepId(7L);
        lot.setRouteSeqNo(10);
        lot.setRouteVersionId(21L);
        lot.setQty(2L);
        lot.setStepEnterAt(LocalDateTime.of(2026, 10, 16, 8, 0));
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(routeNext.next(21L, 10)).thenReturn(Optional.of(new RouteStepNext(20, 9L, 4L)));
        when(stepPass.arrivalPassNo(8L, 9L)).thenReturn(1);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T15:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotSkipApplicationService(transactions, lotRepository, routeNext, stepPass,
                mock(LotHistoryApplicationService.class), mock(FutureActionApplicationService.class), clock);
    }

    @Test
    void skipMovesToNextWithoutCountingSkippedStep() {
        LotSkipRequest request = new LotSkipRequest();
        request.setReasonCode("SKIP");

        service.skip(8L, request);

        assertThat(lot.getStepId()).isEqualTo(9L);
        assertThat(lot.getRouteSeqNo()).isEqualTo(20);
        assertThat(lot.getPassNo()).isEqualTo(1);
        assertThat(lot.getStepEnterAt()).isEqualTo(LocalDateTime.of(2026, 10, 16, 23, 0));
        assertThat(lot.getHoldCount()).isEqualTo(1);
        verify(stepPass, never()).recordTrackOut(any(), any());
    }

    @Test
    void runningLotRejected() {
        lot.setStatus("RUNNING");
        LotSkipRequest request = new LotSkipRequest();
        request.setReasonCode("SKIP");

        assertThatThrownBy(() -> service.skip(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_SKIP_NOT_WAITING);
    }
}
