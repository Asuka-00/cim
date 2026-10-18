package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.dto.LotHoldRequest;
import com.scaffold.mes.exec.domain.entity.FutureAction;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.FutureActionRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;

/**
 * 到达主流程步骤才执行预约。返工中不触发。Hold 失败时动作记为 FAILED，不往外抛。
 */
class FutureActionApplicationServiceTest {

    private FutureActionRepository repository;
    private LotHoldApplicationService holds;
    private FutureAction hold;
    private FutureActionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(FutureActionRepository.class);
        holds = mock(LotHoldApplicationService.class);
        hold = action("HOLD", "Q1");
        when(repository.listDue(8L, 21L, 20)).thenReturn(List.of(hold));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.ofHours(8));
        service = new FutureActionApplicationService(repository, mock(LotRepository.class), holds,
                mock(LotPriorityApplicationService.class), clock);
    }

    @Test
    void arriveHolds() {
        service.onArrive(lot(null));

        verify(holds).hold(eq(8L), org.mockito.ArgumentMatchers.<LotHoldRequest>argThat(row -> "Q1".equals(row.getReasonCode())));
        assertThat(hold.getStatus()).isEqualTo("DONE");
    }

    @Test
    void reworkDoesNotFire() {
        service.onArrive(lot(3L));

        verify(holds, never()).hold(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static Lot lot(Long reworkId) {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(20);
        lot.setPassNo(1);
        lot.setReworkId(reworkId);
        return lot;
    }

    private static FutureAction action(String type, String params) {
        FutureAction action = new FutureAction();
        action.setActionType(type);
        action.setActionParams(params);
        action.setStatus("PENDING");
        return action;
    }
}
