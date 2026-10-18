package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.scaffold.mes.exec.domain.dto.LotReworkRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ReworkRouteProvider;
import com.scaffold.share.integration.mes.base.response.ReworkEntry;

/**
 * 进入返工按根 Lot 计次。子 Lot 不能靠拆批绕过上限。
 */
class LotReworkApplicationServiceTest {

    private LotHistoryRepository historyRepository;
    private Lot lot;
    private LotReworkApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lotRepository = mock(LotRepository.class);
        historyRepository = mock(LotHistoryRepository.class);
        ReworkRouteProvider routes = mock(ReworkRouteProvider.class);
        LotStepPassApplicationService passes = mock(LotStepPassApplicationService.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(20L);
        lot.setRootLotId(8L);
        lot.setStatus("WAITING");
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        lot.setQty(1L);
        when(lotRepository.findById(20L)).thenReturn(Optional.of(lot));
        when(routes.find(3L)).thenReturn(Optional.of(new ReworkEntry(3L, 21L, 10, 20, 30, 1, true, 1, 70L)));
        when(passes.arrivalPassNo(20L, 70L)).thenReturn(1);
        when(historyRepository.countEnterRework(8L, 3L)).thenReturn(0);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotReworkApplicationService(transactions, lotRepository, historyRepository, routes, passes,
                mock(LotHistoryApplicationService.class), clock);
    }

    @Test
    void enterUsesRootCount() {
        LotReworkRequest request = new LotReworkRequest();
        request.setReworkId(3L);

        service.enter(20L, request);

        assertThat(lot.getReworkId()).isEqualTo(3L);
        assertThat(lot.getStepId()).isEqualTo(70L);
        verify(historyRepository).countEnterRework(8L, 3L);
    }

    @Test
    void maxCountRejectsChildLot() {
        when(historyRepository.countEnterRework(8L, 3L)).thenReturn(1);
        LotReworkRequest request = new LotReworkRequest();
        request.setReworkId(3L);

        assertThatThrownBy(() -> service.enter(20L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_REWORK_MAX);
    }
}
