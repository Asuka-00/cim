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
import com.scaffold.mes.exec.domain.dto.LotForceMoveRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

/**
 * 强制跳站可以跨过中间步骤。被跳过的站点不增加经过次数。
 */
class LotForceMoveApplicationServiceTest {

    private LotStepPassApplicationService stepPass;
    private FutureActionApplicationService futureActions;
    private LotRepository lotRepository;
    private ConfirmPort confirms;
    private Lot lot;
    private LotForceMoveApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        stepPass = mock(LotStepPassApplicationService.class);
        futureActions = mock(FutureActionApplicationService.class);
        RouteNextProvider routeNext = mock(RouteNextProvider.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        lot.setStepId(7L);
        lot.setQty(2L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(routeNext.at(21L, 30)).thenReturn(Optional.of(new RouteStepNext(30, 12L, 6L)));
        when(stepPass.arrivalPassNo(8L, 12L)).thenReturn(1);
        confirms = mock(ConfirmPort.class);
        when(confirms.allowed("FORCE_MOVE", "LOT", 8L)).thenReturn(true);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T17:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotForceMoveApplicationService(transactions, lotRepository, routeNext, stepPass,
                mock(LotHistoryApplicationService.class), futureActions, confirms, clock);
    }

    @Test
    void movesWithoutCountingSkippedSteps() {
        service.move(8L, request(30));

        assertThat(lot.getRouteSeqNo()).isEqualTo(30);
        assertThat(lot.getStepId()).isEqualTo(12L);
        assertThat(lot.getLocationId()).isEqualTo(6L);
        verify(stepPass, never()).recordTrackOut(any(), any());
        verify(futureActions).onArrive(lot);
    }

    @Test
    void missingConfirmBlocksMove() {
        when(confirms.allowed("FORCE_MOVE", "LOT", 8L)).thenReturn(false);

        assertThatThrownBy(() -> service.move(8L, request(30)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.CONFIRM_REQUIRED);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void missingStepRejected() {
        assertThatThrownBy(() -> service.move(8L, request(99)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_FORCE_MOVE_STEP_MISSING);
    }

    private static LotForceMoveRequest request(int seq) {
        LotForceMoveRequest request = new LotForceMoveRequest();
        request.setRouteSeqNo(seq);
        request.setReasonCode("MOVE");
        return request;
    }
}
