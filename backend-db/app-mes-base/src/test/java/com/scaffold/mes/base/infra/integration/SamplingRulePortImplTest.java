package com.scaffold.mes.base.infra.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.entity.StepSampling;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteStepSamplingRepository;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.mes.base.domain.repository.StepSamplingRepository;
import com.scaffold.share.integration.mes.base.response.SamplingBinding;

/**
 * 步骤上有抽检覆盖时，不再使用站点绑定。
 */
class SamplingRulePortImplTest {

    private StepSamplingRepository bindings;
    private RouteStepRepository routeSteps;
    private RouteStepSamplingRepository routeSamplings;
    private SamplingRuleRepository rules;
    private SamplingRulePortImpl port;

    @BeforeEach
    void setUp() {
        bindings = mock(StepSamplingRepository.class);
        routeSteps = mock(RouteStepRepository.class);
        routeSamplings = mock(RouteStepSamplingRepository.class);
        rules = mock(SamplingRuleRepository.class);
        when(routeSamplings.listByRouteStep(30L)).thenReturn(List.of());
        port = new SamplingRulePortImpl(bindings, routeSteps, routeSamplings, rules);
    }

    @Test
    void routeOverrideReplacesStationRule() {
        RouteStep step = new RouteStep();
        step.setId(30L);
        when(routeSteps.findBySeq(21L, 20)).thenReturn(Optional.of(step));
        RouteStepSampling row = new RouteStepSampling();
        row.setProductId(0L);
        row.setRuleId(9L);
        when(routeSamplings.listByRouteStep(30L)).thenReturn(List.of(row));
        when(rules.findById(9L)).thenReturn(Optional.of(rule(9L, 5)));
        StepSampling station = new StepSampling();
        station.setRuleId(4L);
        when(bindings.find(7L, 0L)).thenReturn(Optional.of(station));

        Optional<SamplingBinding> binding = port.binding(21L, 20, 7L, null);

        assertThat(binding).contains(new SamplingBinding(9L, 5));
        verify(rules, never()).findById(4L);
    }

    @Test
    void unmatchedProductDoesNotFallBackToStation() {
        RouteStep step = new RouteStep();
        step.setId(30L);
        when(routeSteps.findBySeq(21L, 20)).thenReturn(Optional.of(step));
        RouteStepSampling row = new RouteStepSampling();
        row.setProductId(2L);
        row.setRuleId(9L);
        when(routeSamplings.listByRouteStep(30L)).thenReturn(List.of(row));

        assertThat(port.binding(21L, 20, 7L, 1L)).isEmpty();
        verify(bindings, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private static SamplingRule rule(long id, int n) {
        SamplingRule rule = new SamplingRule();
        rule.setId(id);
        rule.setEnabled(1);
        rule.setNValue(n);
        rule.setSampleType("EVERY_N_LOT");
        return rule;
    }
}
