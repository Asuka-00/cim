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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotMoveBackRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

/**
 * 退回上一步重走。不回减已经记过的经过次数，新的序号是已完成次数加 1。
 */
class LotMoveBackApplicationServiceTest {

    private LotStepPassApplicationService stepPass;
    private RouteNextProvider routeNext;
    private Lot lot;
    private LotMoveBackApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lotRepository = mock(LotRepository.class);
        stepPass = mock(LotStepPassApplicationService.class);
        routeNext = mock(RouteNextProvider.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(20);
        lot.setStepId(9L);
        lot.setPassNo(1);
        lot.setQty(2L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(routeNext.previous(21L, 20)).thenReturn(Optional.of(new RouteStepNext(10, 7L, 4L)));
        when(stepPass.arrivalPassNo(8L, 7L)).thenReturn(2);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T18:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotMoveBackApplicationService(transactions, lotRepository, routeNext, stepPass,
                mock(LotHistoryApplicationService.class), mock(FutureActionApplicationService.class), clock);
    }

    @Test
    void movesBackWithoutDecrementingPass() {
        service.moveBack(8L, request());

        assertThat(lot.getRouteSeqNo()).isEqualTo(10);
        assertThat(lot.getStepId()).isEqualTo(7L);
        assertThat(lot.getPassNo()).isEqualTo(2);
        verify(stepPass, never()).undoTrackOut(any(), any());
    }

    @Test
    void firstStepRejected() {
        when(stepPass.arrivalPassNo(any(), any())).thenReturn(1);
        when(routeNext.previous(21L, 20)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moveBack(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_MOVE_BACK_NO_PREVIOUS);
    }

    private static LotMoveBackRequest request() {
        LotMoveBackRequest request = new LotMoveBackRequest();
        request.setReasonCode("BACK");
        return request;
    }
}
