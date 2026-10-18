package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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
import com.scaffold.mes.exec.domain.dto.LotCancelTrackInRequest;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 加工中剔除会取消进站。最后一个成员离开后批次变为 CANCELLED。取消失败时批次保持 RUNNING。
 */
class BatchEjectApplicationServiceTest {

    private BatchRepository batchRepository;
    private LotRepository lotRepository;
    private LotCancelTrackInApplicationService cancelTrackIn;
    private Batch batch;
    private BatchLot member;
    private Lot lot;
    private BatchEjectApplicationService service;

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        BatchLotRepository members = mock(BatchLotRepository.class);
        lotRepository = mock(LotRepository.class);
        cancelTrackIn = mock(LotCancelTrackInApplicationService.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        batch = new Batch();
        batch.setId(4L);
        batch.setStatus("RUNNING");
        batch.setEquipmentId(3L);
        batch.setLotCount(1);
        batch.setWaferCount(2);
        batch.setTotalQty(2L);
        when(batchRepository.findById(4L)).thenReturn(Optional.of(batch));
        member = new BatchLot();
        member.setLotId(8L);
        member.setStatus("IN");
        when(members.find(4L, 8L)).thenReturn(Optional.of(member));
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("RUNNING");
        lot.setBatchId(4L);
        lot.setQty(2L);
        lot.setWaferCount(2);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        doAnswer(invocation -> {
            lot.setStatus("WAITING");
            return null;
        }).when(cancelTrackIn).cancelFromBatch(eq(8L), any());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T23:30:00Z"), ZoneOffset.ofHours(8));
        service = new BatchEjectApplicationService(transactions, batchRepository, members, lotRepository, cancelTrackIn,
                clock);
    }

    @Test
    void lastMemberLeavesAndCancelsBatch() {
        service.eject(4L, 8L, request());

        verify(cancelTrackIn).cancelFromBatch(eq(8L), any());
        assertThat(lot.getStatus()).isEqualTo("WAITING");
        assertThat(lot.getBatchId()).isNull();
        assertThat(member.getStatus()).isEqualTo("REMOVED");
        assertThat(batch.getStatus()).isEqualTo("CANCELLED");
        assertThat(batch.getLotCount()).isZero();
    }

    @Test
    void cancelFailureLeavesBatchRunning() {
        doThrow(new BusinessException(430221, "blocked")).when(cancelTrackIn).cancelFromBatch(eq(8L), any());

        assertThatThrownBy(() -> service.eject(4L, 8L, request()))
                .isInstanceOf(BusinessException.class);
        assertThat(batch.getStatus()).isEqualTo("RUNNING");
        verify(batchRepository, never()).update(any());
        verify(lotRepository, never()).update(any());
    }

    private static LotCancelTrackInRequest request() {
        LotCancelTrackInRequest request = new LotCancelTrackInRequest();
        request.setReasonCode("EQP");
        return request;
    }
}
