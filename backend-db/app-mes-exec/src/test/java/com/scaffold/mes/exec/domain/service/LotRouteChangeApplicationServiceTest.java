package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.dto.LotRouteChangeRequest;
import com.scaffold.mes.exec.domain.entity.FutureAction;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRouteChange;
import com.scaffold.mes.exec.domain.repository.FutureActionRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotRouteChangeRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

/**
 * 改 Route 取消旧版本上未执行的预约，并停到新 Route 的目标步骤。
 */
class LotRouteChangeApplicationServiceTest {

    private FutureActionRepository futureActionRepository;
    private LotRouteChangeRepository changeRepository;
    private FutureAction oldAction;
    private Lot lot;
    private LotRouteChangeApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lotRepository = mock(LotRepository.class);
        futureActionRepository = mock(FutureActionRepository.class);
        changeRepository = mock(LotRouteChangeRepository.class);
        RouteNextProvider routeNext = mock(RouteNextProvider.class);
        LotStepPassApplicationService passes = mock(LotStepPassApplicationService.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Integer.class))).thenAnswer(invocation -> {
            Supplier<Integer> work = invocation.getArgument(1);
            return work.get();
        });
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        lot.setProductVersionId(11L);
        lot.setQty(2L);
        oldAction = new FutureAction();
        oldAction.setStatus("PENDING");
        oldAction.setRouteVersionId(21L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(futureActionRepository.listPending(8L, 21L)).thenReturn(List.of(oldAction));
        when(routeNext.at(22L, 5)).thenReturn(Optional.of(new RouteStepNext(5, 70L, 4L)));
        when(passes.arrivalPassNo(8L, 70L)).thenReturn(1);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T20:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotRouteChangeApplicationService(transactions, lotRepository, routeNext, futureActionRepository,
                changeRepository, passes, mock(LotHistoryApplicationService.class), mock(FutureActionApplicationService.class),
                session, clock);
    }

    @Test
    void changeCancelsOldFutureActions() {
        LotRouteChangeRequest request = new LotRouteChangeRequest();
        request.setRouteVersionId(22L);
        request.setRouteSeqNo(5);
        request.setReasonCode("ECN");

        assertThat(service.change(8L, request)).isEqualTo(1);

        assertThat(lot.getRouteVersionId()).isEqualTo(22L);
        assertThat(lot.getStepId()).isEqualTo(70L);
        assertThat(oldAction.getStatus()).isEqualTo("CANCELLED");
        verify(changeRepository).save(org.mockito.ArgumentMatchers.<LotRouteChange>argThat(row ->
                row.getFromRouteVersionId().equals(21L) && row.getToSeqNo().equals(5)));
    }
}
