package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.entity.LotSamplingPlan;
import com.scaffold.mes.exec.domain.repository.LotSamplingPlanRepository;

/**
 * 抽中和没抽中都写决定。已有决定时不再推进计数。
 */
class SamplingDecisionApplicationServiceTest {

    private LotSamplingPlanRepository plans;
    private SamplingCounterApplicationService counters;
    private SamplingDecisionApplicationService service;

    @BeforeEach
    void setUp() {
        plans = mock(LotSamplingPlanRepository.class);
        counters = mock(SamplingCounterApplicationService.class);
        when(plans.find(11L, 20, 1)).thenReturn(Optional.empty());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new SamplingDecisionApplicationService(plans, counters, clock);
    }

    @Test
    void missWritesPlan() {
        when(counters.advance(4L, 7L, 0L, 0L, 3, 11L)).thenReturn(false);

        assertThat(service.decide(11L, 20, 1, 7L, 4L, null, null, 3, 2, "1,3,5")).isFalse();

        verify(plans).save(org.mockito.ArgumentMatchers.<LotSamplingPlan>argThat(row ->
                row.getNeedMeasure().equals(0) && row.getWaferSlots() == null && row.getLotId().equals(11L)));
    }

    @Test
    void hitWritesNeedMeasure() {
        when(counters.advance(4L, 7L, 0L, 0L, 3, 11L)).thenReturn(true);

        assertThat(service.decide(11L, 20, 1, 7L, 4L, 0L, 0L, 3, 2, "1,3,5")).isTrue();

        verify(plans).save(org.mockito.ArgumentMatchers.<LotSamplingPlan>argThat(row ->
                row.getNeedMeasure().equals(1) && "1,3,5".equals(row.getWaferSlots()) && row.getWaferCount().equals(2)));
    }

    @Test
    void existingPlanIsReused() {
        LotSamplingPlan plan = new LotSamplingPlan();
        plan.setNeedMeasure(1);
        when(plans.find(11L, 20, 1)).thenReturn(Optional.of(plan));

        assertThat(service.decide(11L, 20, 1, 7L, 4L, 0L, 0L, 3, 2, null)).isTrue();

        verify(counters, never()).advance(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
        verify(plans, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
