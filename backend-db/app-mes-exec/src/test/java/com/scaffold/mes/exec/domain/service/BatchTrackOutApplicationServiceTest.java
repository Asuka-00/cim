package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 批次出站在同一事务里处理每个成员。第二个 Lot 失败时不把批次改成 COMPLETED。
 */
class BatchTrackOutApplicationServiceTest {

    private BatchRepository batchRepository;
    private LotRepository lotRepository;
    private TrackOutApplicationService trackOut;
    private Batch batch;
    private Lot firstLot;
    private Lot secondLot;
    private BatchTrackOutApplicationService service;

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        BatchLotRepository members = mock(BatchLotRepository.class);
        lotRepository = mock(LotRepository.class);
        trackOut = mock(TrackOutApplicationService.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        batch = new Batch();
        batch.setId(4L);
        batch.setStatus("RUNNING");
        batch.setEquipmentId(3L);
        when(batchRepository.findById(4L)).thenReturn(Optional.of(batch));
        when(members.listIn(4L)).thenReturn(List.of(member(8L, 2), member(9L, 1)));
        firstLot = lot(8L);
        secondLot = lot(9L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(firstLot));
        when(lotRepository.findById(9L)).thenReturn(Optional.of(secondLot));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T23:00:00Z"), ZoneOffset.ofHours(8));
        service = new BatchTrackOutApplicationService(transactions, batchRepository, members, lotRepository, trackOut,
                clock);
    }

    @Test
    void trackOutRunsEveryMemberAndClearsBatch() {
        service.trackOut(4L, null);

        verify(trackOut).trackOutFromBatch(9L, null, true);
        verify(trackOut).trackOutFromBatch(8L, null, false);
        assertThat(batch.getStatus()).isEqualTo("COMPLETED");
        assertThat(batch.getTrackOutAt()).isNotNull();
        assertThat(firstLot.getBatchId()).isNull();
        assertThat(secondLot.getBatchId()).isNull();
    }

    @Test
    void secondFailureLeavesBatchRunning() {
        doThrow(new BusinessException(430101, "blocked")).when(trackOut).trackOutFromBatch(eq(9L), isNull(), eq(true));

        assertThatThrownBy(() -> service.trackOut(4L, null))
                .isInstanceOf(BusinessException.class);
        assertThat(batch.getStatus()).isEqualTo("RUNNING");
        verify(batchRepository, never()).update(any());
        verify(lotRepository, never()).update(any());
    }

    private static BatchLot member(Long lotId, int seq) {
        BatchLot member = new BatchLot();
        member.setLotId(lotId);
        member.setSeq(seq);
        member.setStatus("IN");
        return member;
    }

    private static Lot lot(Long id) {
        Lot lot = new Lot();
        lot.setId(id);
        lot.setBatchId(4L);
        return lot;
    }
}
