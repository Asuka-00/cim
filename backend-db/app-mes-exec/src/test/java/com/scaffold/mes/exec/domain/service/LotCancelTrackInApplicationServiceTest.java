package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotCancelTrackInRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;

/**
 * 取消进站回到等待并离开设备。不回减经过次数。
 */
class LotCancelTrackInApplicationServiceTest {

    private LotRepository lotRepository;
    private EquipmentTrackPort equipmentTrack;
    private Lot lot;
    private LotCancelTrackInApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        equipmentTrack = mock(EquipmentTrackPort.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("RUNNING");
        lot.setEquipmentId(3L);
        lot.setRecipeVersionId(9L);
        lot.setPassNo(1);
        lot.setQty(2L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(lotRepository.countRunning(3L)).thenReturn(1L);
        service = new LotCancelTrackInApplicationService(transactions, lotRepository, equipmentTrack,
                mock(LotHistoryApplicationService.class));
    }

    @Test
    void cancelReturnsToWaitingAndReleasesEquipment() {
        service.cancel(8L, request());

        assertThat(lot.getStatus()).isEqualTo("WAITING");
        assertThat(lot.getEquipmentId()).isNull();
        assertThat(lot.getRecipeVersionId()).isNull();
        assertThat(lot.getPassNo()).isEqualTo(1);
        verify(equipmentTrack).releaseIfIdle(3L, 0L);
    }

    @Test
    void waitingLotRejected() {
        lot.setStatus("WAITING");
        lot.setEquipmentId(null);

        assertThatThrownBy(() -> service.cancel(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_CANCEL_TRACK_IN_STATUS);
    }

    private static LotCancelTrackInRequest request() {
        LotCancelTrackInRequest request = new LotCancelTrackInRequest();
        request.setReasonCode("MISS");
        return request;
    }
}
