package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.BatchTrackInRequest;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 批次进站在同一事务里处理每个成员。第二个 Lot 失败时不把批次改成 RUNNING。
 */
class BatchTrackInApplicationServiceTest {

    private BatchRepository batchRepository;
    private TrackInApplicationService trackIn;
    private Batch batch;
    private BatchTrackInApplicationService service;

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        BatchLotRepository members = mock(BatchLotRepository.class);
        trackIn = mock(TrackInApplicationService.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        batch = new Batch();
        batch.setId(4L);
        batch.setStatus("READY");
        batch.setStepId(7L);
        when(batchRepository.findById(4L)).thenReturn(Optional.of(batch));
        BatchLot first = member(8L);
        BatchLot second = member(9L);
        when(members.listIn(4L)).thenReturn(List.of(first, second));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T22:00:00Z"), ZoneOffset.ofHours(8));
        service = new BatchTrackInApplicationService(transactions, batchRepository, members, trackIn, clock);
    }

    @Test
    void trackInRunsEveryMember() {
        service.trackIn(4L, request());

        verify(trackIn).trackInFromBatch(eq(8L), any());
        verify(trackIn).trackInFromBatch(eq(9L), any());
        assertThat(batch.getStatus()).isEqualTo("RUNNING");
        assertThat(batch.getEquipmentId()).isEqualTo(3L);
    }

    @Test
    void secondFailureLeavesBatchReady() {
        doThrow(new BusinessException(430091, "blocked")).when(trackIn).trackInFromBatch(eq(9L), any());

        assertThatThrownBy(() -> service.trackIn(4L, request()))
                .isInstanceOf(BusinessException.class);
        assertThat(batch.getStatus()).isEqualTo("READY");
        verify(batchRepository, never()).update(any());
    }

    @Test
    void belowMinimumRejects() {
        batch.setMinLots(3);

        assertThatThrownBy(() -> service.trackIn(4L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.BATCH_CLOSED);
        verify(trackIn, never()).trackInFromBatch(any(), any());
        assertThat(batch.getStatus()).isEqualTo("READY");
    }

    private static BatchTrackInRequest request() {
        BatchTrackInRequest request = new BatchTrackInRequest();
        request.setEquipmentId(3L);
        return request;
    }

    private static BatchLot member(Long lotId) {
        BatchLot member = new BatchLot();
        member.setLotId(lotId);
        member.setStatus("IN");
        return member;
    }
}
