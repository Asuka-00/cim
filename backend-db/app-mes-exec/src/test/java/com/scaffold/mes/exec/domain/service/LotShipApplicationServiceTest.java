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
import com.scaffold.mes.exec.domain.dto.LotShipRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotShipment;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotShipmentRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 只有已完成且未 Hold 的 Lot 可以出货。出货单号不能重复。
 */
class LotShipApplicationServiceTest {

    private LotRepository lotRepository;
    private LotShipmentRepository shipmentRepository;
    private Lot lot;
    private LotShipApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        shipmentRepository = mock(LotShipmentRepository.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("COMPLETED");
        lot.setHoldCount(0);
        lot.setWoId(4L);
        lot.setQty(2L);
        lot.setQtyUnitId(3L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T13:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotShipApplicationService(transactions, lotRepository, shipmentRepository,
                mock(LotHistoryApplicationService.class), session, clock);
    }

    @Test
    void completedLotShips() {
        service.ship(8L, request());

        assertThat(lot.getStatus()).isEqualTo("SHIPPED");
        verify(shipmentRepository).save(org.mockito.ArgumentMatchers.<LotShipment>argThat(row ->
                "S-1".equals(row.getShipNo()) && row.getQty().equals(2L) && "sam".equals(row.getShippedBy())));
    }

    @Test
    void waitingOrHeldRejected() {
        lot.setStatus("WAITING");
        assertThatThrownBy(() -> service.ship(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_SHIP_STATUS);

        lot.setStatus("COMPLETED");
        lot.setHoldCount(1);
        assertThatThrownBy(() -> service.ship(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_SHIP_HOLD);
        verify(lotRepository, never()).update(any());
    }

    private static LotShipRequest request() {
        LotShipRequest request = new LotShipRequest();
        request.setShipNo("S-1");
        return request;
    }
}
