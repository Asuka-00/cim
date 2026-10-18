package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.integration.mes.base.provider.EquipmentQueueProvider;
import com.scaffold.share.integration.mes.base.provider.UserScopePort;

/**
 * 作业清单按优先级和进站时间排序。指定设备时丢掉该设备做不了的站点。
 */
class WorklistApplicationServiceTest {

    private LotRepository lotRepository;
    private EquipmentQueueProvider equipmentQueue;
    private UserScopePort scopes;
    private WorklistApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        equipmentQueue = mock(EquipmentQueueProvider.class);
        DispatchRuleApplicationService rules = mock(DispatchRuleApplicationService.class);
        when(rules.comparator(null)).thenReturn(java.util.Comparator.comparing(Lot::getPriority)
                .thenComparing(Lot::getStepEnterAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        scopes = mock(UserScopePort.class);
        when(scopes.allowed(eq("sam"), eq("STEP"), any())).thenReturn(true);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        service = new WorklistApplicationService(lotRepository, equipmentQueue, rules, scopes, session);
    }

    @Test
    void sortsByPriorityThenArrival() {
        LocalDateTime earlier = LocalDateTime.of(2026, 10, 16, 8, 0);
        LocalDateTime later = earlier.plusHours(1);
        when(lotRepository.listWaiting(7L, null)).thenReturn(List.of(
                lot(2L, 5, later, 7L),
                lot(1L, 1, later, 7L),
                lot(3L, 5, earlier, 7L)));

        assertThat(service.list(7L, null, null, null)).extracting(item -> item.lotId()).containsExactly(1L, 3L, 2L);
    }

    @Test
    void equipmentKeepsOnlyReachableSteps() {
        when(equipmentQueue.stepIds(3L)).thenReturn(List.of(7L));
        when(lotRepository.listWaiting(null, null)).thenReturn(List.of(
                lot(1L, 1, null, 7L),
                lot(2L, 1, null, 8L)));

        assertThat(service.list(null, null, 3L, null)).extracting(item -> item.lotId()).containsExactly(1L);
    }

    @Test
    void hidesStepsOutsideScope() {
        when(scopes.allowed("sam", "STEP", 7L)).thenReturn(true);
        when(scopes.allowed("sam", "STEP", 8L)).thenReturn(false);
        when(lotRepository.listWaiting(null, null)).thenReturn(List.of(
                lot(1L, 1, null, 7L),
                lot(2L, 1, null, 8L)));

        assertThat(service.list(null, null, null, null)).extracting(item -> item.lotId()).containsExactly(1L);
    }

    @Test
    void hidesAreasOutsideScope() {
        Lot inside = lot(1L, 1, null, 7L);
        inside.setLocationType("AREA");
        inside.setLocationId(4L);
        Lot outside = lot(2L, 1, null, 7L);
        outside.setLocationType("AREA");
        outside.setLocationId(5L);
        when(scopes.allowed("sam", "AREA", 4L)).thenReturn(true);
        when(scopes.allowed("sam", "AREA", 5L)).thenReturn(false);
        when(lotRepository.listWaiting(null, null)).thenReturn(List.of(inside, outside));

        assertThat(service.list(null, null, null, null)).extracting(item -> item.lotId()).containsExactly(1L);
    }

    private static Lot lot(Long id, int priority, LocalDateTime entered, Long stepId) {
        Lot lot = new Lot();
        lot.setId(id);
        lot.setLotNo("L-" + id);
        lot.setPriority(priority);
        lot.setStepId(stepId);
        lot.setStepEnterAt(entered);
        lot.setQty(1L);
        return lot;
    }
}
