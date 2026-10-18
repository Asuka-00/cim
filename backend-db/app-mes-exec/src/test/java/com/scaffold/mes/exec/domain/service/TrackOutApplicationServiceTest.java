package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.mes.exec.domain.dto.TrackOutRequest;
import com.scaffold.share.integration.mes.base.provider.BomConsumePort;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.mes.base.provider.ToolLifePort;
import com.scaffold.share.integration.mes.base.response.BomConsumeLine;
import com.scaffold.share.integration.mes.warehouse.provider.EquipmentBindingPort;
import com.scaffold.share.integration.mes.warehouse.provider.LinesideStockPort;
import com.scaffold.share.integration.mes.warehouse.response.BindingDeduction;
import com.scaffold.share.integration.mes.warehouse.response.StockDeduction;
import com.scaffold.share.integration.mes.base.provider.ReworkRouteProvider;
import com.scaffold.share.integration.mes.base.provider.CollectSpecPort;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.provider.SamplingRulePort;
import com.scaffold.share.integration.mes.base.response.SamplingBinding;
import com.scaffold.share.integration.mes.base.response.ReworkProgress;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

/**
 * 出站加经过次数并清空设备。有下一步则回到 WAITING，没有则完成。Hold 不阻止出站。
 */
class TrackOutApplicationServiceTest {

    private LotRepository lotRepository;
    private LotStepPassApplicationService stepPass;
    private RouteNextProvider routeNext;
    private ReworkRouteProvider reworkRoute;
    private EquipmentTrackPort equipmentTrack;
    private MesTransactionService transactions;
    private Lot lot;
    private ToolLifePort toolLife;
    private BomConsumePort bomConsume;
    private LinesideStockPort stock;
    private EquipmentBindingPort bindings;
    private MaterialConsumptionApplicationService consumptions;
    private SamplingRulePort samplingRules;
    private SamplingDecisionApplicationService samplingDecisions;
    private CollectSpecPort collectSpecs;
    private CollectResultApplicationService collects;
    private TrackOutApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        stepPass = mock(LotStepPassApplicationService.class);
        routeNext = mock(RouteNextProvider.class);
        reworkRoute = mock(ReworkRouteProvider.class);
        equipmentTrack = mock(EquipmentTrackPort.class);
        transactions = mock(MesTransactionService.class);
        LotHistoryApplicationService history = mock(LotHistoryApplicationService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = running();
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(lotRepository.countRunning(3L)).thenReturn(1L);
        when(stepPass.arrivalPassNo(8L, 9L)).thenReturn(1);
        when(routeNext.next(21L, 10)).thenReturn(Optional.of(new RouteStepNext(20, 9L, 4L)));
        toolLife = mock(ToolLifePort.class);
        bomConsume = mock(BomConsumePort.class);
        stock = mock(LinesideStockPort.class);
        bindings = mock(EquipmentBindingPort.class);
        consumptions = mock(MaterialConsumptionApplicationService.class);
        samplingRules = mock(SamplingRulePort.class);
        samplingDecisions = mock(SamplingDecisionApplicationService.class);
        collectSpecs = mock(CollectSpecPort.class);
        collects = mock(CollectResultApplicationService.class);
        when(collectSpecs.requiredItems(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.nullable(Long.class)))
                .thenReturn(java.util.List.of());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new TrackOutApplicationService(transactions, lotRepository, stepPass, routeNext, reworkRoute, equipmentTrack, history,
                mock(FutureActionApplicationService.class), mock(UnitConversionApplicationService.class),
                mock(LotTimeLinkTimerApplicationService.class), toolLife, bomConsume, stock, bindings, consumptions,
                samplingRules, samplingDecisions, collectSpecs, collects, clock);
    }

    @Test
    void trackOutMovesToNextStepAndClearsEquipment() {
        service.trackOut(8L, null);

        assertThat(lot.getStatus()).isEqualTo("WAITING");
        assertThat(lot.getStepId()).isEqualTo(9L);
        assertThat(lot.getRouteSeqNo()).isEqualTo(20);
        assertThat(lot.getEquipmentId()).isNull();
        assertThat(lot.getLocationType()).isEqualTo("AREA");
        assertThat(lot.getHoldCount()).isEqualTo(1);
        verify(toolLife, never()).use(any());
        verify(stepPass).recordTrackOut(8L, 7L);
        verify(equipmentTrack).releaseIfIdle(3L, 0L);
        verify(transactions).publish(eq("LOT_TRACK_OUT"), eq("LOT"), eq(8L), isNull(), any());
        verify(transactions).publish(eq("WORK_ORDER_PROGRESS"), eq("WORK_ORDER"), eq(5L), isNull(), any());
        org.mockito.ArgumentCaptor<MesTxnCommand> command = org.mockito.ArgumentCaptor.forClass(MesTxnCommand.class);
        verify(transactions).execute(command.capture(), any(), eq(Boolean.class));
        assertThat(command.getValue().getLocks()).extracting(MesLockTarget::kind).containsExactly("EQUIPMENT", "LOT");
    }

    @Test
    void boundMaterialSkipsLineside() {
        lot.setBomVersionId(4L);
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of(new BomConsumeLine(15L, new BigDecimal("1.5"))));
        when(bindings.deduct(3L, 15L, new BigDecimal("1.5"))).thenReturn(new BindingDeduction(9L, 20L, 8L));

        service.trackOut(8L, null);

        verify(stock, never()).deduct(any(), any());
        verify(consumptions).recordBound(lot, 15L, new BigDecimal("1.5"), "PER_LOT", new BindingDeduction(9L, 20L, 8L));
    }

    @Test
    void trackOutDeductsPerLotMaterial() {
        lot.setBomVersionId(4L);
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of(new BomConsumeLine(15L, new BigDecimal("1.5"))));
        when(stock.deduct(15L, new BigDecimal("1.5"))).thenReturn(new StockDeduction(20L, 3L, 8L));

        service.trackOut(8L, null);

        verify(stock).deduct(15L, new BigDecimal("1.5"));
        verify(consumptions).record(lot, 15L, new BigDecimal("1.5"), "PER_LOT", new StockDeduction(20L, 3L, 8L), false);
        assertThat(lot.getStatus()).isEqualTo("WAITING");
    }

    @Test
    void trackOutDeductsPerWaferMaterial() {
        lot.setBomVersionId(4L);
        lot.setWaferCount(2);
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of());
        when(bomConsume.perWafer(4L, 7L)).thenReturn(List.of(new BomConsumeLine(16L, new BigDecimal("0.5"))));
        when(stock.deduct(16L, new BigDecimal("1.0"))).thenReturn(new StockDeduction(21L, 3L, 8L));

        service.trackOut(8L, null);

        verify(stock).deduct(16L, new BigDecimal("1.0"));
        verify(consumptions).record(eq(lot), eq(16L), eq(new BigDecimal("1.0")), eq("PER_WAFER"),
                eq(new StockDeduction(21L, 3L, 8L)), eq(false));
    }

    @Test
    void trackOutDeductsPerDieMaterial() {
        lot.setBomVersionId(4L);
        lot.setQty(4L);
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of());
        when(bomConsume.perDie(4L, 7L)).thenReturn(List.of(new BomConsumeLine(17L, new BigDecimal("2"))));
        when(stock.deduct(17L, new BigDecimal("8"))).thenReturn(new StockDeduction(22L, 3L, 8L));

        service.trackOut(8L, null);

        verify(consumptions).record(eq(lot), eq(17L), eq(new BigDecimal("8")), eq("PER_DIE"),
                eq(new StockDeduction(22L, 3L, 8L)), eq(false));
    }

    @Test
    void trackOutDeductsPerStripMaterial() {
        lot.setBomVersionId(4L);
        lot.setSubQty(3L);
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of());
        when(bomConsume.perStrip(4L, 7L)).thenReturn(List.of(new BomConsumeLine(18L, new BigDecimal("2"))));
        when(stock.deduct(18L, new BigDecimal("6"))).thenReturn(new StockDeduction(23L, 3L, 8L));

        service.trackOut(8L, null);

        verify(consumptions).record(eq(lot), eq(18L), eq(new BigDecimal("6")), eq("PER_STRIP"),
                eq(new StockDeduction(23L, 3L, 8L)), eq(false));
    }

    @Test
    void shortStockStillTracksOutAsShortage() {
        lot.setBomVersionId(4L);
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of(new BomConsumeLine(15L, new BigDecimal("1.5"))));
        when(stock.deduct(15L, new BigDecimal("1.5"))).thenReturn(null);

        service.trackOut(8L, null);

        assertThat(lot.getStatus()).isEqualTo("WAITING");
        verify(consumptions).record(lot, 15L, new BigDecimal("1.5"), "PER_LOT", new StockDeduction(0L, 0L, 0L), true);
    }

    @Test
    void trackOutDeductsPerTimeMaterial() {
        lot.setBomVersionId(4L);
        lot.setTrackInAt(LocalDateTime.of(2026, 10, 16, 15, 0));
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of());
        when(bomConsume.perTime(4L, 7L)).thenReturn(List.of(new BomConsumeLine(19L, new BigDecimal("0.5"))));
        when(stock.deduct(19L, new BigDecimal("30.0"))).thenReturn(new StockDeduction(24L, 3L, 8L));

        service.trackOut(8L, null);

        verify(consumptions).record(eq(lot), eq(19L), eq(new BigDecimal("30.0")), eq("PER_TIME"),
                eq(new StockDeduction(24L, 3L, 8L)), eq(false));
    }

    @Test
    void laterBatchMemberSkipsPerTime() {
        lot.setBomVersionId(4L);
        lot.setTrackInAt(LocalDateTime.of(2026, 10, 16, 15, 0));
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of());
        when(bomConsume.perTime(4L, 7L)).thenReturn(List.of(new BomConsumeLine(19L, new BigDecimal("0.5"))));

        service.trackOutFromBatch(8L, null, false);

        verify(consumptions, never()).record(eq(lot), eq(19L), any(), eq("PER_TIME"), any(), eq(false));
        assertThat(lot.getStatus()).isEqualTo("WAITING");
    }

    @Test
    void trackOutDeductsPerRunOnce() {
        lot.setBomVersionId(4L);
        when(bomConsume.perLot(4L, 7L)).thenReturn(List.of());
        when(bomConsume.perRun(4L, 7L)).thenReturn(List.of(new BomConsumeLine(30L, new BigDecimal("1"))));
        when(stock.deduct(30L, new BigDecimal("1"))).thenReturn(new StockDeduction(25L, 3L, 8L));

        service.trackOut(8L, null);

        verify(consumptions).record(eq(lot), eq(30L), eq(new BigDecimal("1")), eq("PER_RUN"),
                eq(new StockDeduction(25L, 3L, 8L)), eq(false));
    }

    @Test
    void trackOutCountsToolUse() {
        TrackOutRequest request = new TrackOutRequest();
        request.setToolId(4L);

        service.trackOut(8L, request);

        verify(toolLife).use(4L);
        assertThat(lot.getStatus()).isEqualTo("WAITING");
    }

    @Test
    void lastStepCompletesLot() {
        when(routeNext.next(21L, 10)).thenReturn(Optional.empty());

        service.trackOut(8L, null);

        assertThat(lot.getStatus()).isEqualTo("COMPLETED");
        assertThat(lot.getStepId()).isEqualTo(7L);
        assertThat(lot.getFinishedAt()).isNotNull();
    }

    @Test
    void lastReworkStepReturnsToMain() {
        lot.setReworkId(3L);
        lot.setReworkSeqNo(2);
        when(reworkRoute.nextAfter(3L, 2)).thenReturn(Optional.of(new ReworkProgress(true, null, 30, 11L, 6L)));
        when(stepPass.arrivalPassNo(8L, 11L)).thenReturn(1);

        service.trackOut(8L, null);

        assertThat(lot.getStatus()).isEqualTo("WAITING");
        assertThat(lot.getReworkId()).isNull();
        assertThat(lot.getRouteSeqNo()).isEqualTo(30);
        assertThat(lot.getStepId()).isEqualTo(11L);
    }

    @Test
    void waitingLotRejected() {
        lot.setStatus("WAITING");

        assertThatThrownBy(() -> service.trackOut(8L, null))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_NOT_RUNNING);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void unsampledSamplingStepIsSkipped() {
        when(routeNext.sampling(21L, 20)).thenReturn(true);
        when(samplingRules.binding(21L, 20, 9L, null)).thenReturn(Optional.of(new SamplingBinding(4L, 3)));
        when(samplingDecisions.decide(8L, 20, 1, 9L, 4L, 0L, null, 3, null, null)).thenReturn(false);
        when(routeNext.next(21L, 20)).thenReturn(Optional.of(new RouteStepNext(30, 11L, 4L)));
        when(stepPass.arrivalPassNo(8L, 11L)).thenReturn(1);

        service.trackOut(8L, null);

        assertThat(lot.getStepId()).isEqualTo(11L);
        assertThat(lot.getRouteSeqNo()).isEqualTo(30);
    }

    @Test
    void sampledSamplingStepIsKept() {
        when(routeNext.sampling(21L, 20)).thenReturn(true);
        when(samplingRules.binding(21L, 20, 9L, null)).thenReturn(Optional.of(new SamplingBinding(4L, 3)));
        when(samplingDecisions.decide(8L, 20, 1, 9L, 4L, 0L, null, 3, null, null)).thenReturn(true);

        service.trackOut(8L, null);

        assertThat(lot.getStepId()).isEqualTo(9L);
        assertThat(lot.getRouteSeqNo()).isEqualTo(20);
    }

    @Test
    void missingRequiredCollectRejected() {
        lot.setPassNo(1);
        when(collectSpecs.requiredItems(21L, 10, 7L, null)).thenReturn(java.util.List.of(15L));
        when(collects.effective(8L, 15L, 10, 1)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.trackOut(8L, null))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.COLLECT_REQUIRED);
        verify(lotRepository, never()).update(any());
    }

    private static Lot running() {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setLotNo("L-1");
        lot.setWoId(5L);
        lot.setStatus("RUNNING");
        lot.setHoldCount(1);
        lot.setStepId(7L);
        lot.setRouteSeqNo(10);
        lot.setRouteVersionId(21L);
        lot.setEquipmentId(3L);
        lot.setQty(2L);
        return lot;
    }
}
