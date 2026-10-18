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
import com.scaffold.mes.exec.domain.dto.LotTerminateRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;

/**
 * 终止保留数量。Hold 不阻止。已完成的 Lot 不能再终止。
 */
class LotTerminateApplicationServiceTest {

    private Lot lot;
    private EquipmentTrackPort equipmentTrack;
    private LotTerminateApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lotRepository = mock(LotRepository.class);
        equipmentTrack = mock(EquipmentTrackPort.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("RUNNING");
        lot.setHoldCount(1);
        lot.setQty(2L);
        lot.setScrapQty(0L);
        lot.setEquipmentId(3L);
        lot.setWoId(4L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(lotRepository.countRunning(3L)).thenReturn(1L);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T14:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotTerminateApplicationService(transactions, lotRepository, equipmentTrack,
                mock(LotHistoryApplicationService.class), clock);
    }

    @Test
    void terminateKeepsQty() {
        LotTerminateRequest request = new LotTerminateRequest();
        request.setReasonCode("STOP");

        service.terminate(8L, request);

        assertThat(lot.getStatus()).isEqualTo("TERMINATED");
        assertThat(lot.getQty()).isEqualTo(2L);
        assertThat(lot.getScrapQty()).isZero();
        assertThat(lot.getEquipmentId()).isNull();
        verify(equipmentTrack).releaseIfIdle(3L, 0L);
    }

    @Test
    void completedLotRejected() {
        lot.setStatus("COMPLETED");
        lot.setEquipmentId(null);
        LotTerminateRequest request = new LotTerminateRequest();
        request.setReasonCode("STOP");

        assertThatThrownBy(() -> service.terminate(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_TERMINATE_CLOSED);
    }
}
