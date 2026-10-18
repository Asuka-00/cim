package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.TrackInRequest;
import com.scaffold.mes.exec.domain.entity.InterlockLog;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.InterlockLogRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.service.interlock.InterlockEngine;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentGroupPort;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.mes.base.provider.QueueTimeProvider;
import com.scaffold.share.integration.mes.base.provider.StepToolPort;
import com.scaffold.share.integration.mes.base.provider.ToolLifePort;
import com.scaffold.share.integration.mes.base.provider.TrackResourceProvider;
import com.scaffold.share.integration.mes.base.provider.UserQualPort;
import com.scaffold.share.integration.mes.base.provider.UserScopePort;
import com.scaffold.share.integration.mes.base.response.QueueTimeLimit;
import com.scaffold.share.integration.mes.base.response.TrackResourceFacts;

/**
 * 进站先锁设备再锁 Lot。Hold、停机和满载会阻断。STANDBY 上的生产批把设备联动为 PRODUCTIVE。
 */
class TrackInApplicationServiceTest {

    private LotRepository lotRepository;
    private TrackResourceProvider resources;
    private EquipmentTrackPort equipmentTrack;
    private LotHistoryApplicationService history;
    private GenealogyLinkApplicationService genealogy;
    private MesTransactionService transactions;
    private Lot lot;
    private UserQualPort qualifications;
    private UserScopePort scopes;
    private QueueTimeProvider queueTimes;
    private LotHoldApplicationService holds;
    private ToolLifePort toolLife;
    private StepToolPort stepTools;
    private EquipmentGroupPort equipmentGroups;
    private InterlockLogRepository interlockLogs;
    private TrackInApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        resources = mock(TrackResourceProvider.class);
        equipmentTrack = mock(EquipmentTrackPort.class);
        history = mock(LotHistoryApplicationService.class);
        genealogy = mock(GenealogyLinkApplicationService.class);
        transactions = mock(MesTransactionService.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = lot();
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(lotRepository.countOccupancy(eq(3L), isNull())).thenReturn(0L);
        UserQualPort qualifications = mock(UserQualPort.class);
        when(qualifications.allowed(eq("sam"), eq("EQUIPMENT"), eq(3L), any())).thenReturn(true);
        when(qualifications.allowed(eq("sam"), eq("STEP"), eq(7L), any())).thenReturn(true);
        when(qualifications.allowed(eq("sam"), eq("EQUIPMENT_GROUP"), eq(9L), any())).thenReturn(true);
        this.qualifications = qualifications;
        UserScopePort scopes = mock(UserScopePort.class);
        when(scopes.allowed(eq("sam"), eq("STEP"), eq(7L))).thenReturn(true);
        when(scopes.allowed(eq("sam"), eq("EQUIPMENT_GROUP"), nullable(Long.class))).thenReturn(true);
        this.scopes = scopes;
        queueTimes = mock(QueueTimeProvider.class);
        when(queueTimes.limit(any(), any())).thenReturn(new QueueTimeLimit(null, null));
        holds = mock(LotHoldApplicationService.class);
        ToolLifePort toolLife = mock(ToolLifePort.class);
        when(toolLife.withinLife(4L)).thenReturn(true);
        this.toolLife = toolLife;
        stepTools = mock(StepToolPort.class);
        when(stepTools.accepted(eq(7L), eq(1L), nullable(Long.class), eq(21L), eq(10))).thenReturn(true);
        equipmentGroups = mock(EquipmentGroupPort.class);
        when(equipmentGroups.groupId(3L)).thenReturn(9L);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T07:00:00Z"), ZoneOffset.ofHours(8));
        interlockLogs = mock(InterlockLogRepository.class);
        service = new TrackInApplicationService(transactions, lotRepository, resources,
                new InterlockEngine(lotRepository, interlockLogs, session, clock),
                equipmentTrack, history, genealogy, session, mock(LotTimeLinkTimerApplicationService.class),
                qualifications, scopes, queueTimes, holds, toolLife, stepTools, equipmentGroups, clock);
    }

    @Test
    void trackInMovesLotToRunningAndLinksStandbyEquipment() {
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        service.trackIn(8L, request());

        assertThat(lot.getStatus()).isEqualTo("RUNNING");
        assertThat(lot.getLocationType()).isEqualTo("EQUIPMENT");
        verify(interlockLogs, never()).save(any());
        verify(equipmentTrack).onTrackIn(3L, "PRODUCTIVE");
        verify(history).record(eq(lot), eq("TRACK_IN"), isNull(), isNull(), eq("{\"status\":\"WAITING\",\"stepId\":7}"),
                eq("WAITING"), eq(2L));
        verify(genealogy).link(eq("LOT"), eq(8L), eq("L-1"), eq("EQUIPMENT"), eq(3L), isNull(), eq("PROCESS"),
                eq(7L), any(), isNull());
        ArgumentCaptor<MesTxnCommand> command = ArgumentCaptor.forClass(MesTxnCommand.class);
        verify(transactions).execute(command.capture(), any(), eq(Boolean.class));
        assertThat(command.getValue().getLocks()).extracting(MesLockTarget::kind)
                .containsExactly("EQUIPMENT", "LOT");
    }

    @Test
    void holdBlocksTrackIn() {
        lot.setHoldCount(1);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.INTERLOCK_BLOCK);
        verify(lotRepository, never()).update(any());
        verify(interlockLogs).save(org.mockito.ArgumentMatchers.<InterlockLog>argThat(row ->
                "BLOCK".equals(row.getResult()) && row.getDetail().startsWith("LOT_STATE") && row.getLotId().equals(8L)));
    }

    @Test
    void scheduledDownAllowsMonitorWithoutChangingEquipment() {
        lot.setLotType("MONITOR");
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("SCHEDULED_DOWN", "MANUAL", 2));

        service.trackIn(8L, request());

        assertThat(lot.getStatus()).isEqualTo("RUNNING");
        verify(equipmentTrack, never()).onTrackIn(any(), any());
    }

    @Test
    void fullEquipmentBlocks() {
        when(lotRepository.countOccupancy(eq(3L), isNull())).thenReturn(2L);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.INTERLOCK_BLOCK);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void expiredQualificationBlocks() {
        when(qualifications.allowed(eq("sam"), eq("EQUIPMENT"), eq(3L), any())).thenReturn(false);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.PERSON_NOT_QUALIFIED);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void stepOutOfScopeBlocks() {
        when(scopes.allowed(eq("sam"), eq("STEP"), eq(7L))).thenReturn(false);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.STEP_OUT_OF_SCOPE);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void overdueQueueHoldsAndBlocks() {
        lot.setStepEnterAt(LocalDateTime.of(2026, 10, 16, 14, 0));
        when(queueTimes.limit(21L, 10)).thenReturn(new QueueTimeLimit(30, "HOLD"));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.QUEUE_TIME_EXCEEDED);
        verify(holds).holdSystem(8L, "QUEUE_TIME", "QUEUE_TIME", 7L);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void groupQualificationBlocks() {
        when(qualifications.allowed(eq("sam"), eq("EQUIPMENT_GROUP"), eq(9L), any())).thenReturn(false);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.GROUP_NOT_QUALIFIED);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void stepQualificationBlocks() {
        when(qualifications.allowed(eq("sam"), eq("STEP"), eq(7L), any())).thenReturn(false);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.STEP_NOT_QUALIFIED);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void equipmentGroupOutOfScopeBlocks() {
        when(scopes.allowed("sam", "EQUIPMENT_GROUP", 9L)).thenReturn(false);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        assertThatThrownBy(() -> service.trackIn(8L, request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.EQUIPMENT_GROUP_OUT_OF_SCOPE);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void wrongToolTypeBlocksTrackIn() {
        when(stepTools.accepted(7L, 1L, 4L, 21L, 10)).thenReturn(false);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));
        TrackInRequest request = request();
        request.setToolId(4L);

        assertThatThrownBy(() -> service.trackIn(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.TOOL_TYPE_MISMATCH);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void wornToolBlocksTrackIn() {
        when(toolLife.withinLife(4L)).thenReturn(false);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));
        TrackInRequest request = request();
        request.setToolId(4L);

        assertThatThrownBy(() -> service.trackIn(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.TOOL_LIFE_EXCEEDED);
        verify(lotRepository, never()).update(any());
    }

    @Test
    void warnQueueStillTracksIn() {
        lot.setStepEnterAt(LocalDateTime.of(2026, 10, 16, 14, 0));
        when(queueTimes.limit(21L, 10)).thenReturn(new QueueTimeLimit(30, "WARN"));
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 2));

        service.trackIn(8L, request());

        assertThat(lot.getStatus()).isEqualTo("RUNNING");
        verify(holds, never()).holdSystem(any(), any(), any(), any());
    }

    @Test
    void batchMemberUsesOneSlot() {
        lot.setBatchId(4L);
        when(lotRepository.countOccupancy(3L, 4L)).thenReturn(0L);
        when(resources.facts(any(), any(), any(), any(), any(), nullable(Long.class), nullable(Long.class)))
                .thenReturn(facts("STANDBY", "MANUAL", 1));

        service.trackInFromBatch(8L, request());

        assertThat(lot.getStatus()).isEqualTo("RUNNING");
    }

    private static TrackResourceFacts facts(String state, String source, int capacity) {
        return new TrackResourceFacts(true, false, true, state, source, capacity, null, false);
    }

    private static TrackInRequest request() {
        TrackInRequest request = new TrackInRequest();
        request.setStepId(7L);
        request.setEquipmentId(3L);
        return request;
    }

    private static Lot lot() {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setLotNo("L-1");
        lot.setLotType("PRODUCTION");
        lot.setStatus("WAITING");
        lot.setHoldCount(0);
        lot.setStepId(7L);
        lot.setQty(2L);
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        lot.setProductId(1L);
        return lot;
    }
}
