package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepSamplingBindRequest;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.entity.StepSampling;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.mes.base.domain.repository.StepSamplingRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 启用的规则可以绑到站点。停用规则或同一站点与产品已有绑定则拒绝。
 */
class StepSamplingApplicationServiceTest {

    private SamplingRuleRepository rules;
    private StepSamplingRepository bindings;
    private StepSamplingApplicationService service;

    @BeforeEach
    void setUp() {
        rules = mock(SamplingRuleRepository.class);
        bindings = mock(StepSamplingRepository.class);
        when(bindings.find(7L, 0L)).thenReturn(Optional.empty());
        service = new StepSamplingApplicationService(rules, bindings);
    }

    @Test
    void enabledRuleBindsAsGeneralProduct() {
        when(rules.findById(4L)).thenReturn(Optional.of(enabled()));

        service.bind(request(null));

        verify(bindings).save(org.mockito.ArgumentMatchers.<StepSampling>argThat(row ->
                row.getStepId().equals(7L) && row.getProductId().equals(0L) && row.getRuleId().equals(4L)));
    }

    @Test
    void disabledRuleRejected() {
        SamplingRule rule = enabled();
        rule.setEnabled(0);
        when(rules.findById(4L)).thenReturn(Optional.of(rule));

        assertThatThrownBy(() -> service.bind(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_SAMPLING_INVALID);
    }

    @Test
    void duplicateStepAndProductRejected() {
        when(rules.findById(4L)).thenReturn(Optional.of(enabled()));
        when(bindings.find(7L, 0L)).thenReturn(Optional.of(new StepSampling()));

        assertThatThrownBy(() -> service.bind(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_SAMPLING_INVALID);
    }

    private SamplingRule enabled() {
        SamplingRule rule = new SamplingRule();
        rule.setEnabled(1);
        return rule;
    }

    private StepSamplingBindRequest request(Long productId) {
        StepSamplingBindRequest request = new StepSamplingBindRequest();
        request.setStepId(7L);
        request.setProductId(productId);
        request.setRuleId(4L);
        return request;
    }
}
