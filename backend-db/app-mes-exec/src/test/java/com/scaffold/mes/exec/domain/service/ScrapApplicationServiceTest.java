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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotScrapRequest;
import com.scaffold.mes.exec.domain.dto.WaferScrapRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.system.DictItemLookup;

/**
 * 片级报废后数量等于存活片数。最后一片报废时 Lot 变为 SCRAPPED。没有片时按数量报废。
 */
class ScrapApplicationServiceTest {

    private LotRepository lotRepository;
    private WaferRepository waferRepository;
    private EquipmentTrackPort equipmentTrack;
    private LotRelationRepository relations;
    private ConfirmPort confirms;
    private Lot lot;
    private Wafer first;
    private Wafer second;
    private ScrapApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        waferRepository = mock(WaferRepository.class);
        equipmentTrack = mock(EquipmentTrackPort.class);
        DictItemLookup reasons = mock(DictItemLookup.class);
        when(reasons.enabled("MES_SCRAP_REASON", "BROKEN")).thenReturn(true);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = lot();
        first = wafer(1L);
        second = wafer(2L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(lotRepository.countRunning(3L)).thenReturn(1L);
        when(waferRepository.findById(1L)).thenReturn(Optional.of(first));
        when(waferRepository.listByLot(8L)).thenReturn(List.of(first, second));
        LotRelationRepository relations = mock(LotRelationRepository.class);
        when(relations.existsPackageInput(8L)).thenReturn(false);
        ConfirmPort confirms = mock(ConfirmPort.class);
        when(confirms.allowed("SCRAP", "LOT", 8L)).thenReturn(true);
        this.confirms = confirms;
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T09:00:00Z"), ZoneOffset.ofHours(8));
        service = new ScrapApplicationService(transactions, lotRepository, waferRepository, reasons,
                mock(WaferHistoryApplicationService.class), mock(LotHistoryApplicationService.class), equipmentTrack,
                relations, confirms, clock);
        this.relations = relations;
    }

    @Test
    void scrapOneWaferReducesQty() {
        WaferScrapRequest request = new WaferScrapRequest();
        request.setReasonCode("BROKEN");

        service.scrapWafer(1L, request);

        assertThat(first.getStatus()).isEqualTo("SCRAPPED");
        assertThat(lot.getQty()).isEqualTo(1L);
        assertThat(lot.getStatus()).isEqualTo("WAITING");
        assertThat(lot.getWaferCount()).isEqualTo(1);
        verify(lotRepository, never()).update(org.mockito.ArgumentMatchers.<Lot>argThat(row -> "SCRAPPED".equals(row.getStatus()) && row == lot && lot.getQty() == 0));
    }

    @Test
    void scrapLotScrapsRemainingWafers() {
        lot.setStatus("RUNNING");
        first.setStatus("SCRAPPED");
        LotScrapRequest request = new LotScrapRequest();
        request.setReasonCode("BROKEN");

        service.scrapLot(8L, request);

        assertThat(second.getStatus()).isEqualTo("SCRAPPED");
        assertThat(lot.getStatus()).isEqualTo("SCRAPPED");
        assertThat(lot.getQty()).isZero();
        assertThat(lot.getEquipmentId()).isNull();
        verify(equipmentTrack).releaseIfIdle(3L, 0L);
    }

    @Test
    void missingConfirmBlocksScrap() {
        when(confirms.allowed("SCRAP", "LOT", 8L)).thenReturn(false);
        LotScrapRequest request = new LotScrapRequest();
        request.setReasonCode("BROKEN");

        assertThatThrownBy(() -> service.scrapLot(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.CONFIRM_REQUIRED);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void dieScrapRejectsBareGoodCountAboveQty() {
        lot.setWaferCount(0);
        when(waferRepository.listByLot(8L)).thenReturn(List.of());
        LotScrapRequest request = new LotScrapRequest();
        request.setReasonCode("BROKEN");
        request.setScrapQty(3L);

        assertThatThrownBy(() -> service.scrapLot(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.SCRAP_QTY_INVALID);
    }

    @Test
    void packageSourceScrapToZeroIsConsumed() {
        lot.setWaferCount(0);
        when(waferRepository.listByLot(8L)).thenReturn(List.of());
        when(relations.existsPackageInput(8L)).thenReturn(true);
        LotScrapRequest request = new LotScrapRequest();
        request.setReasonCode("BROKEN");
        request.setScrapQty(2L);

        service.scrapLot(8L, request);

        assertThat(lot.getQty()).isZero();
        assertThat(lot.getStatus()).isEqualTo("CONSUMED");
    }

    @Test
    void unknownReasonRejected() {
        LotScrapRequest request = new LotScrapRequest();
        request.setReasonCode("NOPE");

        assertThatThrownBy(() -> service.scrapLot(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.SCRAP_REASON_INVALID);
    }

    private static Lot lot() {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setWoId(5L);
        lot.setStatus("WAITING");
        lot.setHoldCount(1);
        lot.setQty(2L);
        lot.setWaferCount(2);
        lot.setStepId(7L);
        lot.setEquipmentId(3L);
        lot.setScrapQty(0L);
        lot.setCurrentScrapQty(0L);
        return lot;
    }

    private static Wafer wafer(Long id) {
        Wafer wafer = new Wafer();
        wafer.setId(id);
        wafer.setLotId(8L);
        wafer.setStatus("NORMAL");
        wafer.setSlotNo(id.intValue());
        wafer.setWaferCode("W" + id);
        return wafer;
    }
}
