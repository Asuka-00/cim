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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotMergeRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 合批要求经过次数一致且都没有 Hold。源 Lot 数量清零并变为 MERGED。
 */
class LotMergeApplicationServiceTest {

    private LotRepository lotRepository;
    private LotStepPassRepository passes;
    private Lot target;
    private Lot source;
    private Wafer wafer;
    private LotMergeApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        WaferRepository wafers = mock(WaferRepository.class);
        passes = mock(LotStepPassRepository.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        target = lot(8L, 1L);
        source = lot(9L, 1L);
        wafer = new Wafer();
        wafer.setId(1L);
        wafer.setLotId(9L);
        wafer.setStatus("NORMAL");
        when(lotRepository.findById(8L)).thenReturn(Optional.of(target));
        when(lotRepository.findById(9L)).thenReturn(Optional.of(source));
        when(wafers.listByLot(9L)).thenReturn(List.of(wafer));
        when(passes.listByLot(8L)).thenReturn(List.of(pass(8L, 2)));
        when(passes.listByLot(9L)).thenReturn(List.of(pass(9L, 2)));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T11:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotMergeApplicationService(transactions, lotRepository, wafers, passes,
                mock(LotRelationRepository.class), mock(LotHistoryApplicationService.class),
                mock(WaferHistoryApplicationService.class), mock(GenealogyLinkApplicationService.class), clock);
    }

    @Test
    void mergeMovesQtyAndZeroesSource() {
        service.merge(8L, request());

        assertThat(target.getQty()).isEqualTo(2L);
        assertThat(target.getInitialQty()).isEqualTo(2L);
        assertThat(source.getQty()).isZero();
        assertThat(source.getInitialQty()).isZero();
        assertThat(source.getStatus()).isEqualTo("MERGED");
        assertThat(wafer.getLotId()).isEqualTo(8L);
    }

    @Test
    void batchLotCannotMerge() {
        source.setBatchId(4L);

        assertThatThrownBy(() -> service.merge(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.BATCH_LOT_BUSY);
        assertThat(source.getStatus()).isEqualTo("WAITING");
    }

    @Test
    void holdOrDifferentPassRejects() {
        source.setHoldCount(1);
        assertThatThrownBy(() -> service.merge(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_MERGE_REJECTED);

        source.setHoldCount(0);
        when(passes.listByLot(9L)).thenReturn(List.of(pass(9L, 1)));
        assertThatThrownBy(() -> service.merge(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_MERGE_REJECTED);
        verify(lotRepository, org.mockito.Mockito.never()).update(org.mockito.ArgumentMatchers.<Lot>argThat(
                lot -> "MERGED".equals(lot.getStatus())));
    }

    private static LotMergeRequest request() {
        LotMergeRequest request = new LotMergeRequest();
        request.setSourceLotId(9L);
        return request;
    }

    private static Lot lot(Long id, long qty) {
        Lot lot = new Lot();
        lot.setId(id);
        lot.setLotNo("L-" + id);
        lot.setStatus("WAITING");
        lot.setHoldCount(0);
        lot.setWoId(5L);
        lot.setProductVersionId(11L);
        lot.setRouteVersionId(21L);
        lot.setStepId(7L);
        lot.setRouteSeqNo(10);
        lot.setQtyUnitId(3L);
        lot.setQty(qty);
        lot.setInitialQty(qty);
        lot.setScrapQty(0L);
        lot.setCurrentScrapQty(0L);
        lot.setWaferCount(1);
        return lot;
    }

    private static LotStepPass pass(Long lotId, int count) {
        LotStepPass pass = new LotStepPass();
        pass.setLotId(lotId);
        pass.setStepId(7L);
        pass.setPassCount(count);
        return pass;
    }
}
