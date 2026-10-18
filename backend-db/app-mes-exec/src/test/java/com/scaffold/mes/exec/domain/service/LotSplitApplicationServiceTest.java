package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotSplitRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotHoldRepository;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 拆批后子 Lot 继承步骤和 Hold，父 Lot 至少留下一片。子 Lot 号用父号加点号和序号。
 */
class LotSplitApplicationServiceTest {

    private LotRepository lotRepository;
    private LotHoldRepository holdRepository;
    private Lot parent;
    private Wafer first;
    private Wafer second;
    private LotSplitApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        WaferRepository waferRepository = mock(WaferRepository.class);
        holdRepository = mock(LotHoldRepository.class);
        LotStepPassRepository passes = mock(LotStepPassRepository.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(String.class))).thenAnswer(invocation -> {
            Supplier<String> work = invocation.getArgument(1);
            return work.get();
        });
        parent = parent();
        first = wafer(1L);
        second = wafer(2L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(parent));
        when(lotRepository.maxSplitSeq(8L)).thenReturn(0);
        when(waferRepository.listByLot(8L)).thenReturn(List.of(first, second));
        LotStepPass pass = new LotStepPass();
        pass.setLotId(8L);
        pass.setStepId(7L);
        pass.setPassCount(2);
        when(passes.listByLot(8L)).thenReturn(List.of(pass));
        LotHold hold = new LotHold();
        hold.setLotId(8L);
        hold.setStatus("ACTIVE");
        hold.setActiveKey("8|10|INTERLOCK|Q1");
        hold.setReasonCode("Q1");
        hold.setSourceRefId(0L);
        when(holdRepository.listActive(8L)).thenReturn(List.of(hold));
        doAnswer(invocation -> {
            Lot child = invocation.getArgument(0);
            child.setId(20L);
            return null;
        }).when(lotRepository).save(any());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T10:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotSplitApplicationService(transactions, lotRepository, waferRepository, holdRepository, passes,
                mock(LotRelationRepository.class), mock(LotHistoryApplicationService.class),
                mock(WaferHistoryApplicationService.class), mock(GenealogyLinkApplicationService.class), clock);
    }

    @Test
    void splitKeepsHoldAndLeavesOneWafer() {
        LotSplitRequest request = new LotSplitRequest();
        request.setWaferIds(List.of(1L));

        assertThat(service.split(8L, request)).isEqualTo("L-1.1");

        assertThat(parent.getQty()).isEqualTo(1L);
        assertThat(parent.getInitialQty()).isEqualTo(1L);
        assertThat(first.getLotId()).isEqualTo(20L);
        verify(holdRepository).save(org.mockito.ArgumentMatchers.<LotHold>argThat(row ->
                row.getLotId().equals(20L) && "20|10|INTERLOCK|Q1".equals(row.getActiveKey())));
        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(row ->
                row.getRootLotId().equals(8L)
                        && row.getStepEnterAt().equals(parent.getStepEnterAt())
                        && row.getLocationType() == null
                        && row.getPassNo().equals(1)));
    }

    @Test
    void batchLotCannotSplit() {
        parent.setBatchId(4L);
        LotSplitRequest request = new LotSplitRequest();
        request.setWaferIds(List.of(1L));

        assertThatThrownBy(() -> service.split(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.BATCH_LOT_BUSY);
        assertThat(parent.getQty()).isEqualTo(2L);
    }

    @Test
    void cannotSplitEveryWafer() {
        LotSplitRequest request = new LotSplitRequest();
        request.setWaferIds(List.of(1L, 2L));

        assertThatThrownBy(() -> service.split(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_SPLIT_QTY_INVALID);
    }

    private static Lot parent() {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setLotNo("L-1");
        lot.setLotType("PRODUCTION");
        lot.setStatus("WAITING");
        lot.setHoldCount(1);
        lot.setQty(2L);
        lot.setInitialQty(2L);
        lot.setWaferCount(2);
        lot.setStepId(7L);
        lot.setRouteSeqNo(10);
        lot.setPassNo(1);
        lot.setPriority(5);
        lot.setStepEnterAt(LocalDateTime.of(2026, 10, 16, 8, 0));
        lot.setProductId(1L);
        lot.setProductVersionId(11L);
        lot.setRouteVersionId(21L);
        lot.setQtyUnitId(3L);
        return lot;
    }

    private static Wafer wafer(Long id) {
        Wafer wafer = new Wafer();
        wafer.setId(id);
        wafer.setLotId(8L);
        wafer.setStatus("NORMAL");
        wafer.setSlotNo(id.intValue());
        return wafer;
    }
}
