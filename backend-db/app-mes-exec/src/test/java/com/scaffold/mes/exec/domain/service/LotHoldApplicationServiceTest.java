package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotHoldRequest;
import com.scaffold.mes.exec.domain.dto.LotReleaseRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.mes.exec.domain.repository.LotHoldRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;

/**
 * 手工 Hold 可叠加。系统 Hold 按幂等键只记一条。Release 减 hold_count，并校验角色。
 */
class LotHoldApplicationServiceTest {

    private LotHoldRepository holdRepository;
    private LotRepository lotRepository;
    private ReleaseRoleChecker roleChecker;
    private ConfirmPort confirms;
    private Lot lot;
    private LotHoldApplicationService service;

    @BeforeEach
    void setUp() {
        holdRepository = mock(LotHoldRepository.class);
        lotRepository = mock(LotRepository.class);
        HoldReasonRepository reasonRepository = mock(HoldReasonRepository.class);
        roleChecker = mock(ReleaseRoleChecker.class);
        LotHistoryApplicationService history = mock(LotHistoryApplicationService.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setHoldCount(0);
        lot.setStepId(7L);
        lot.setRouteSeqNo(10);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(reasonRepository.findByCode("Q1")).thenReturn(Optional.of(reason(null)));
        when(reasonRepository.findByCode("Q2")).thenReturn(Optional.of(reason("QA")));
        ConfirmPort confirms = mock(ConfirmPort.class);
        when(confirms.allowed("HOLD_RELEASE", "LOT", 8L)).thenReturn(true);
        this.confirms = confirms;
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T05:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotHoldApplicationService(
                holdRepository, lotRepository, reasonRepository, roleChecker, history, session, confirms, clock);
    }

    @Test
    void manualHoldStacksWithoutActiveKey() {
        service.hold(8L, request());
        service.hold(8L, request());

        ArgumentCaptor<LotHold> saved = ArgumentCaptor.forClass(LotHold.class);
        verify(holdRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(LotHold::getActiveKey).containsOnlyNulls();
        assertThat(lot.getHoldCount()).isEqualTo(2);
        verify(lotRepository, times(2)).lock(8L);
    }

    @Test
    void systemHoldWithSameKeyInsertsOnce() {
        when(holdRepository.findActiveByKey("8|10|INTERLOCK|Q1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new LotHold()));

        service.holdSystem(8L, "INTERLOCK", "Q1", null);
        service.holdSystem(8L, "INTERLOCK", "Q1", null);

        verify(holdRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
        assertThat(lot.getHoldCount()).isEqualTo(1);
    }

    @Test
    void releaseClearsKeyAndDecrements() {
        LotHold hold = active("8|10|OOS|Q1");
        when(holdRepository.findById(3L)).thenReturn(Optional.of(hold));
        lot.setHoldCount(1);
        LotReleaseRequest request = new LotReleaseRequest();
        request.setHoldId(3L);

        service.release(8L, request);

        assertThat(hold.getStatus()).isEqualTo("RELEASED");
        assertThat(hold.getActiveKey()).isNull();
        assertThat(lot.getHoldCount()).isZero();
    }

    @Test
    void missingConfirmBlocksRelease() {
        when(confirms.allowed("HOLD_RELEASE", "LOT", 8L)).thenReturn(false);
        LotReleaseRequest request = new LotReleaseRequest();
        request.setHoldId(3L);

        assertThatThrownBy(() -> service.release(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.CONFIRM_REQUIRED);
        verify(holdRepository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void releaseRejectsMissingRole() {
        LotHold hold = active(null);
        hold.setReleaseRoleCode("QA");
        when(holdRepository.findById(3L)).thenReturn(Optional.of(hold));
        when(roleChecker.has("QA")).thenReturn(false);
        LotReleaseRequest request = new LotReleaseRequest();
        request.setHoldId(3L);

        assertThatThrownBy(() -> service.release(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_HOLD_ROLE_DENIED);
        verify(holdRepository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void releaseSystemClearsMatchingHold() {
        lot.setStatus("RUNNING");
        lot.setHoldCount(1);
        when(holdRepository.findActiveByKey("8|10|OOS|OOS")).thenReturn(Optional.of(active("8|10|OOS|OOS")));

        service.releaseSystem(8L, "OOS", "OOS");

        assertThat(lot.getHoldCount()).isZero();
        verify(holdRepository).update(org.mockito.ArgumentMatchers.<LotHold>argThat(row ->
                "RELEASED".equals(row.getStatus()) && row.getActiveKey() == null));
        verify(confirms, never()).allowed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private static LotHoldRequest request() {
        LotHoldRequest request = new LotHoldRequest();
        request.setReasonCode("Q1");
        return request;
    }

    private static HoldReason reason(String role) {
        HoldReason reason = new HoldReason();
        reason.setReasonCode("Q1");
        reason.setHoldType("QUALITY");
        reason.setReleaseRoleCode(role);
        reason.setEnabled(1);
        return reason;
    }

    private static LotHold active(String key) {
        LotHold hold = new LotHold();
        hold.setId(3L);
        hold.setLotId(8L);
        hold.setStatus("ACTIVE");
        hold.setActiveKey(key);
        return hold;
    }
}
