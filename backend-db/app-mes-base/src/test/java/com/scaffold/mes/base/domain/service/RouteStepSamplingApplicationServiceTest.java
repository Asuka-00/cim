package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteStepSamplingSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteStepSamplingRepository;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 启用的间隔规则可以覆盖到 Route 步骤。步骤不存在或已有覆盖时拒绝。
 */
class RouteStepSamplingApplicationServiceTest {

    private RouteStepRepository routeSteps;
    private RouteStepSamplingRepository samplings;
    private SamplingRuleRepository rules;
    private RouteStepSamplingApplicationService service;

    @BeforeEach
    void setUp() {
        routeSteps = mock(RouteStepRepository.class);
        samplings = mock(RouteStepSamplingRepository.class);
        rules = mock(SamplingRuleRepository.class);
        RouteStep step = new RouteStep();
        step.setId(30L);
        when(routeSteps.findBySeq(21L, 20)).thenReturn(Optional.of(step));
        when(samplings.listByRouteStep(30L)).thenReturn(List.of());
        when(rules.findById(9L)).thenReturn(Optional.of(enabled()));
        service = new RouteStepSamplingApplicationService(routeSteps, samplings, rules);
    }

    @Test
    void savesGeneralOverride() {
        service.save(request(null));

        verify(samplings).save(org.mockito.ArgumentMatchers.<RouteStepSampling>argThat(row ->
                row.getRouteStepId().equals(30L) && row.getProductId().equals(0L) && row.getRuleId().equals(9L)));
    }

    @Test
    void missingStepRejected() {
        when(routeSteps.findBySeq(21L, 20)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_SAMPLING_INVALID);
    }

    @Test
    void duplicateProductRejected() {
        RouteStepSampling existing = new RouteStepSampling();
        existing.setProductId(0L);
        when(samplings.listByRouteStep(30L)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.save(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_SAMPLING_INVALID);
    }

    private static SamplingRule enabled() {
        SamplingRule rule = new SamplingRule();
        rule.setEnabled(1);
        rule.setSampleType("EVERY_N_LOT");
        return rule;
    }

    private static RouteStepSamplingSaveRequest request(Long productId) {
        RouteStepSamplingSaveRequest request = new RouteStepSamplingSaveRequest();
        request.setRouteVersionId(21L);
        request.setSeqNo(20);
        request.setProductId(productId);
        request.setRuleId(9L);
        return request;
    }
}
