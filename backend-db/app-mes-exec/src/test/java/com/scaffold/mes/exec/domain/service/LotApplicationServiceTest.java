package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotStartRequest;
import com.scaffold.mes.exec.domain.dto.WipImportRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.entity.LotTimeLinkTimer;
import com.scaffold.mes.exec.domain.repository.LotTimeLinkTimerRepository;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.mes.exec.domain.repository.LotHoldRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.provider.RouteStartProvider;
import com.scaffold.share.integration.mes.base.provider.TimeLinkProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;
import com.scaffold.share.integration.mes.base.response.RouteStepStart;
import com.scaffold.share.integration.mes.base.response.TimeLinkStart;

/**
 * 开批停在第一步。提交写入时，当前站点不在工单工艺路线上不写 Lot。
 */
class LotApplicationServiceTest {

    private LotRepository lotRepository;
    private WorkOrderRepository workOrderRepository;
    private LotStepPassRepository passes;
    private LotHoldRepository holds;
    private HoldReasonRepository holdReasons;
    private TimeLinkProvider timeLinks;
    private LotTimeLinkTimerRepository timers;
    private WaferRepository wafers;
    private RouteNextProvider routeNext;
    private LotApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        workOrderRepository = mock(WorkOrderRepository.class);
        RouteStartProvider routeStartProvider = mock(RouteStartProvider.class);
        when(routeStartProvider.first(21L)).thenReturn(new RouteStepStart(10, 7L));
        when(workOrderRepository.findById(4L)).thenReturn(Optional.of(released(25L)));
        when(lotRepository.sumInitialQty(4L)).thenReturn(20L);
        passes = mock(LotStepPassRepository.class);
        holds = mock(LotHoldRepository.class);
        holdReasons = mock(HoldReasonRepository.class);
        timeLinks = mock(TimeLinkProvider.class);
        timers = mock(LotTimeLinkTimerRepository.class);
        wafers = mock(WaferRepository.class);
        when(wafers.listByLot(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        routeNext = mock(RouteNextProvider.class);
        when(routeNext.at(21L, 30)).thenReturn(Optional.of(new RouteStepNext(30, 9L, null)));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotApplicationService(lotRepository, workOrderRepository, routeStartProvider, routeNext, passes,
                holds, holdReasons, timeLinks, timers, wafers, clock);
    }

    @Test
    void startPlacesLotOnFirstStepWithoutTouchingWorkOrderCounters() {
        service.start(4L, request(5L));

        verify(workOrderRepository).lock(4L);
        verify(workOrderRepository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot -> "WAITING".equals(lot.getStatus())
                && lot.getRouteSeqNo().equals(10)
                && lot.getStepId().equals(7L)
                && lot.getRouteVersionId().equals(21L)
                && lot.getInitialQty().equals(5L)
                && lot.getPassNo().equals(1)));
    }

    @Test
    void startRejectsWhenPlanWouldBeExceeded() {
        assertThatThrownBy(() -> service.start(4L, request(6L)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_OVER_PLAN);
        verify(lotRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void startRejectsCreatedWorkOrder() {
        WorkOrder created = released(25L);
        created.setStatus("CREATED");
        when(workOrderRepository.findById(4L)).thenReturn(Optional.of(created));

        assertThatThrownBy(() -> service.start(4L, request(1L)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_WO_NOT_RELEASED);
    }

    @Test
    void importStopsAtGivenStep() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(5L);
        request.setSeqNo(30);
        request.setStepId(9L);

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                "WAITING".equals(lot.getStatus())
                        && Integer.valueOf(30).equals(lot.getRouteSeqNo())
                        && Long.valueOf(9L).equals(lot.getStepId())
                        && "IMPORT".equals(lot.getSource())
                        && Integer.valueOf(1).equals(lot.getPassNo())));
    }

    @Test
    void importWritesStepEnterAndLastTrackOut() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(5L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        request.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T15:00"));

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                LocalDateTime.parse("2026-10-16T16:00").equals(lot.getStepEnterAt())
                        && LocalDateTime.parse("2026-10-16T15:00").equals(lot.getLastTrackOutAt())));
    }

    @Test
    void importUsesNowWhenStepEnterMissing() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(5L);
        request.setSeqNo(30);
        request.setStepId(9L);

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                LocalDateTime.parse("2026-10-16T12:00").equals(lot.getStepEnterAt())));
    }

    @Test
    void importLeavesLastTrackOutEmptyWhenMissing() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(5L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                lot.getLastTrackOutAt() == null
                        && LocalDateTime.parse("2026-10-16T16:00").equals(lot.getStepEnterAt())));
    }

    @Test
    void importRejectsStepOffRoute() {
        when(routeNext.at(21L, 30)).thenReturn(Optional.of(new RouteStepNext(30, 8L, null)));
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(5L);
        request.setSeqNo(30);
        request.setStepId(9L);

        assertThatThrownBy(() -> service.importWip(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WIP_STEP);
        verify(lotRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void importWritesPassCount() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Lot.class).setId(8L);
            return null;
        }).when(lotRepository).save(org.mockito.ArgumentMatchers.any());
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(5L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setPassCount(2);

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                Integer.valueOf(3).equals(lot.getPassNo())));
        verify(passes).save(org.mockito.ArgumentMatchers.<LotStepPass>argThat(row ->
                Long.valueOf(8L).equals(row.getLotId())
                        && Long.valueOf(9L).equals(row.getStepId())
                        && Integer.valueOf(2).equals(row.getPassCount())));
    }

    @Test
    void importWritesPriorStepPass() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Lot.class).setId(8L);
            return null;
        }).when(lotRepository).save(org.mockito.ArgumentMatchers.any());
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(5L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setPriorStepId(4L);
        request.setPriorPassCount(1);

        service.importWip(4L, request);

        verify(passes).save(org.mockito.ArgumentMatchers.<LotStepPass>argThat(row ->
                Long.valueOf(4L).equals(row.getStepId()) && Integer.valueOf(1).equals(row.getPassCount())));
    }

    @Test
    void importRejectsPriorStepWithoutCount() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setPriorStepId(4L);

        assertThatThrownBy(() -> service.importWip(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WIP_STEP);
    }

    @Test
    void importOpensOneHold() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Lot.class).setId(8L);
            return null;
        }).when(lotRepository).save(org.mockito.ArgumentMatchers.any());
        HoldReason reason = new HoldReason();
        reason.setReasonCode("QC");
        reason.setHoldType("QUALITY");
        reason.setEnabled(1);
        when(holdReasons.findByCode("QC")).thenReturn(Optional.of(reason));
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setHoldReasonCode("QC");

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                Integer.valueOf(1).equals(lot.getHoldCount())));
        verify(holds).save(org.mockito.ArgumentMatchers.<LotHold>argThat(row ->
                Long.valueOf(8L).equals(row.getLotId())
                        && "QC".equals(row.getReasonCode())
                        && "ACTIVE".equals(row.getStatus())
                        && "IMPORT".equals(row.getHoldSource())));
    }

    @Test
    void importUsesGivenStepEnterTime() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setStepEnterAt(LocalDateTime.parse("2026-10-15T08:00"));

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                LocalDateTime.parse("2026-10-15T08:00").equals(lot.getStepEnterAt())
                        && LocalDateTime.parse("2026-10-16T12:00").equals(lot.getStartedAt())));
    }

    @Test
    void importUsesGivenLastTrackOut() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setLastTrackOutAt(LocalDateTime.parse("2026-10-15T18:00"));

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                LocalDateTime.parse("2026-10-15T18:00").equals(lot.getLastTrackOutAt())));
    }

    @Test
    void importStoresReworkPosition() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setReworkId(6L);
        request.setReworkSeqNo(2);

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                Long.valueOf(6L).equals(lot.getReworkId()) && Integer.valueOf(2).equals(lot.getReworkSeqNo())));
    }

    @Test
    void importStoresLocation() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setLocationId(12L);

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                Long.valueOf(12L).equals(lot.getLocationId())));
    }

    @Test
    void importStoresRecipeOverride() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setRecipeVersionId(15L);

        service.importWip(4L, request);

        verify(lotRepository).save(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                Long.valueOf(15L).equals(lot.getRecipeVersionId())));
    }

    @Test
    void importOpensTimeLink() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Lot.class).setId(8L);
            return null;
        }).when(lotRepository).save(org.mockito.ArgumentMatchers.any());
        when(timeLinks.find(21L, 4L)).thenReturn(Optional.of(new TimeLinkStart(4L, 10, 30, 60, "WARN", null)));
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setTimeLinkId(4L);
        request.setTimeLinkStartAt(LocalDateTime.parse("2026-10-15T10:00"));

        service.importWip(4L, request);

        verify(timers).save(org.mockito.ArgumentMatchers.<LotTimeLinkTimer>argThat(row ->
                Long.valueOf(8L).equals(row.getLotId())
                        && "ACTIVE".equals(row.getStatus())
                        && LocalDateTime.parse("2026-10-15T10:00").equals(row.getStartAt())
                        && LocalDateTime.parse("2026-10-15T11:00").equals(row.getDeadlineAt())));
    }

    @Test
    void importCreatesWafer() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Lot.class).setId(8L);
            return null;
        }).when(lotRepository).save(org.mockito.ArgumentMatchers.any());
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setWaferCode("W-1");
        request.setSlotNo(3);

        service.importWip(4L, request);

        verify(wafers).save(org.mockito.ArgumentMatchers.<Wafer>argThat(row ->
                "W-1".equals(row.getWaferCode()) && Integer.valueOf(3).equals(row.getSlotNo())
                        && Long.valueOf(8L).equals(row.getLotId())));
        verify(lotRepository).update(org.mockito.ArgumentMatchers.<Lot>argThat(lot ->
                Integer.valueOf(1).equals(lot.getWaferCount())));
    }

    @Test
    void importRejectsDuplicateSlot() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Lot.class).setId(8L);
            return null;
        }).when(lotRepository).save(org.mockito.ArgumentMatchers.any());
        Wafer existing = new Wafer();
        existing.setSlotNo(3);
        when(wafers.listByLot(8L)).thenReturn(List.of(existing));
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setWaferCode("W-1");
        request.setSlotNo(3);

        assertThatThrownBy(() -> service.importWip(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WIP_STEP);
    }

    @Test
    void importRejectsReworkWithoutSeq() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setReworkId(6L);

        assertThatThrownBy(() -> service.importWip(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WIP_STEP);
    }

    @Test
    void importRejectsStepEnterBeforeLastTrackOut() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        request.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T17:00"));

        assertThatThrownBy(() -> service.importWip(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WIP_STEP);
        verify(lotRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void importRejectsDuplicateLotNo() {
        when(lotRepository.findByNo("L-9")).thenReturn(Optional.of(new Lot()));
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);

        assertThatThrownBy(() -> service.importWip(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_NO_EXISTS);
    }

    @Test
    void importScrappedWaferNeedsReason() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Lot.class).setId(8L);
            return null;
        }).when(lotRepository).save(org.mockito.ArgumentMatchers.any());
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setWaferCode("W-1");
        request.setSlotNo(3);
        request.setWaferStatus("SCRAPPED");
        request.setScrapReason("BROKEN");

        service.importWip(4L, request);

        verify(wafers).save(org.mockito.ArgumentMatchers.<Wafer>argThat(row ->
                "SCRAPPED".equals(row.getStatus()) && "BROKEN".equals(row.getScrapReason())));
        verify(lotRepository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void importRejectsScrapWithoutReason() {
        WipImportRequest request = new WipImportRequest();
        request.setLotNo("L-9");
        request.setQty(1L);
        request.setSeqNo(30);
        request.setStepId(9L);
        request.setWaferCode("W-1");
        request.setSlotNo(3);
        request.setWaferStatus("SCRAPPED");

        assertThatThrownBy(() -> service.importWip(4L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WIP_STEP);
    }

    private static WorkOrder released(long plan) {
        WorkOrder order = new WorkOrder();
        order.setId(4L);
        order.setStatus("RELEASED");
        order.setPlanQty(plan);
        order.setProductId(1L);
        order.setProductVersionId(11L);
        order.setRouteVersionId(21L);
        order.setBomVersionId(31L);
        order.setQtyUnitId(3L);
        order.setPriority(5);
        return order;
    }

    private static LotStartRequest request(long qty) {
        LotStartRequest request = new LotStartRequest();
        request.setLotNo("L-1");
        request.setQty(qty);
        return request;
    }
}
