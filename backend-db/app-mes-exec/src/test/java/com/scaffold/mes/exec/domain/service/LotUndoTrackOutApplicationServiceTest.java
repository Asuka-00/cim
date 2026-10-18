package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotUndoTrackOutRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotSamplingPlanRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.mes.base.provider.ToolLifePort;
import com.scaffold.share.integration.mes.base.provider.TrackResourceProvider;
import com.scaffold.share.integration.mes.base.response.TrackResourceFacts;

/**
 * 撤销出站回到出站前的步骤和设备，并回减那一次经过。设备已满时拒绝。
 */
class LotUndoTrackOutApplicationServiceTest {

    private LotRepository lotRepository;
    private LotStepPassApplicationService stepPass;
    private EquipmentTrackPort equipmentTrack;
    private ToolLifePort toolLife;
    private MaterialConsumptionApplicationService consumptions;
    private LotSamplingPlanRepository samplingPlans;
    private Lot lot;
    private LotUndoTrackOutApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        LotHistoryRepository historyRepository = mock(LotHistoryRepository.class);
        stepPass = mock(LotStepPassApplicationService.class);
        equipmentTrack = mock(EquipmentTrackPort.class);
        TrackResourceProvider resources = mock(TrackResourceProvider.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setStepId(9L);
        lot.setRouteSeqNo(20);
        lot.setRouteVersionId(21L);
        lot.setProductId(1L);
        lot.setQty(2L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(lotRepository.countRunning(3L)).thenReturn(0L);
        when(resources.facts(eq(7L), eq(21L), eq(20), eq(1L), eq(3L), nullable(Long.class), nullable(Long.class)))
                .thenReturn(new TrackResourceFacts(true, false, true, "STANDBY", "MANUAL", 1, null, false));
        LotHistory trackOut = new LotHistory();
        trackOut.setBeforeJson("""
                {"status":"RUNNING","stepId":7,"seq":10,"equipmentId":3,"portId":null,"recipeVersionId":null,"stepEnterAt":"2026-10-16T08:00"}""");
        when(historyRepository.findLatest(8L, "TRACK_OUT")).thenReturn(Optional.of(trackOut));
        toolLife = mock(ToolLifePort.class);
        consumptions = mock(MaterialConsumptionApplicationService.class);
        samplingPlans = mock(LotSamplingPlanRepository.class);
        service = new LotUndoTrackOutApplicationService(transactions, lotRepository, historyRepository, stepPass, resources,
                equipmentTrack, mock(LotHistoryApplicationService.class), toolLife, consumptions, samplingPlans);
    }

    @Test
    void undoRestoresRunningAndDecrementsPass() {
        service.undo(8L, request());

        assertThat(lot.getStatus()).isEqualTo("RUNNING");
        assertThat(lot.getStepId()).isEqualTo(7L);
        assertThat(lot.getRouteSeqNo()).isEqualTo(10);
        assertThat(lot.getEquipmentId()).isEqualTo(3L);
        verify(stepPass).undoTrackOut(8L, 7L);
        verify(equipmentTrack).onTrackIn(3L, "PRODUCTIVE");
        verify(toolLife, never()).reverse(any());
        verify(consumptions).reverse(lot, 7L);
        verify(samplingPlans).deleteAfter(8L, 10);
    }

    @Test
    void undoReversesToolUse() {
        LotUndoTrackOutRequest request = request();
        request.setToolId(4L);

        service.undo(8L, request);

        verify(toolLife).reverse(4L);
        assertThat(lot.getStatus()).isEqualTo("RUNNING");
    }

    @Test
    void fullEquipmentRejected() {
        when(lotRepository.countRunning(3L)).thenReturn(1L);

        assertThatThrownBy(() -> service.undo(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_UNDO_TRACK_OUT_CAPACITY);
        verify(stepPass, never()).undoTrackOut(any(), any());
    }

    private static LotUndoTrackOutRequest request() {
        LotUndoTrackOutRequest request = new LotUndoTrackOutRequest();
        request.setReasonCode("UNDO");
        return request;
    }
}
