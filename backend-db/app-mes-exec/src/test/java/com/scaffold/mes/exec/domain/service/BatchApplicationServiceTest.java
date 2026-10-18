package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 组批只收同站点、未 Hold 的等待 Lot。满员后拒绝。移出后清空 Lot 上的批次。
 */
class BatchApplicationServiceTest {

    private BatchRepository batchRepository;
    private BatchLotRepository memberRepository;
    private LotRepository lotRepository;
    private Batch batch;
    private Lot lot;
    private BatchApplicationService service;

    @BeforeEach
    void setUp() {
        batchRepository = mock(BatchRepository.class);
        memberRepository = mock(BatchLotRepository.class);
        lotRepository = mock(LotRepository.class);
        batch = new Batch();
        batch.setId(4L);
        batch.setStepId(7L);
        batch.setStatus("OPEN");
        batch.setMaxLots(1);
        batch.setLotCount(0);
        batch.setWaferCount(0);
        batch.setTotalQty(0L);
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setHoldCount(0);
        lot.setStepId(7L);
        lot.setQty(2L);
        lot.setWaferCount(2);
        when(batchRepository.findById(4L)).thenReturn(Optional.of(batch));
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T21:00:00Z"), ZoneOffset.ofHours(8));
        service = new BatchApplicationService(batchRepository, memberRepository, lotRepository, clock);
    }

    @Test
    void addMarksReadyAndRejectsWhenFull() {
        service.add(4L, 8L);

        assertThat(batch.getStatus()).isEqualTo("READY");
        assertThat(batch.getLotCount()).isEqualTo(1);
        assertThat(lot.getBatchId()).isEqualTo(4L);
        verify(memberRepository).save(org.mockito.ArgumentMatchers.<BatchLot>argThat(row -> "IN".equals(row.getStatus())));

        assertThatThrownBy(() -> service.add(4L, 9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.BATCH_CLOSED);
    }

    @Test
    void belowMinimumStaysOpen() {
        batch.setMinLots(2);
        batch.setMaxLots(2);

        service.add(4L, 8L);

        assertThat(batch.getStatus()).isEqualTo("OPEN");
        assertThat(batch.getLotCount()).isEqualTo(1);
    }

    @Test
    void differentRecipeRejected() {
        batch.setMaxLots(2);
        lot.setRecipeVersionId(5L);
        service.add(4L, 8L);
        assertThat(batch.getRecipeVersionId()).isEqualTo(5L);

        Lot other = new Lot();
        other.setId(9L);
        other.setStatus("WAITING");
        other.setHoldCount(0);
        other.setStepId(7L);
        other.setQty(1L);
        other.setRecipeVersionId(9L);
        when(lotRepository.findById(9L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.add(4L, 9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.BATCH_RECIPE_MISMATCH);
        assertThat(batch.getLotCount()).isEqualTo(1);
    }

    @Test
    void heldLotRejected() {
        lot.setHoldCount(1);

        assertThatThrownBy(() -> service.add(4L, 8L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.BATCH_LOT_NOT_READY);
    }

    @Test
    void cancelClearsMembers() {
        batch.setStatus("READY");
        batch.setLotCount(1);
        lot.setBatchId(4L);
        BatchLot member = new BatchLot();
        member.setLotId(8L);
        member.setStatus("IN");
        when(memberRepository.listIn(4L)).thenReturn(List.of(member));

        service.cancel(4L);

        assertThat(batch.getStatus()).isEqualTo("CANCELLED");
        assertThat(batch.getLotCount()).isZero();
        assertThat(lot.getBatchId()).isNull();
        assertThat(member.getStatus()).isEqualTo("REMOVED");
    }

    @Test
    void runningBatchCannotCancel() {
        batch.setStatus("RUNNING");

        assertThatThrownBy(() -> service.cancel(4L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.BATCH_CLOSED);
    }
}
