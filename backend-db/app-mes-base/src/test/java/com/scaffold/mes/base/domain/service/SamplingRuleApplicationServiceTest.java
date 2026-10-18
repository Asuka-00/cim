package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.SamplingRuleCreateRequest;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 每 N 个 Lot 抽 1 个。间隔小于 1 或编码重复时拒绝。
 */
class SamplingRuleApplicationServiceTest {

    private SamplingRuleRepository repository;
    private SamplingRuleApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(SamplingRuleRepository.class);
        when(repository.findByCode("SPC-3")).thenReturn(Optional.empty());
        service = new SamplingRuleApplicationService(repository);
    }

    @Test
    void everyThirdLotIsSaved() {
        SamplingRuleCreateRequest request = request(3);
        service.create(request);
        verify(repository).save(org.mockito.ArgumentMatchers.<SamplingRule>argThat(row ->
                "EVERY_N_LOT".equals(row.getSampleType()) && row.getNValue().equals(3) && row.getEnabled().equals(1)));
    }

    @Test
    void intervalBelowOneRejected() {
        assertThatThrownBy(() -> service.create(request(0)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.SAMPLING_RULE_INVALID);
    }

    @Test
    void duplicateCodeRejected() {
        when(repository.findByCode("SPC-3")).thenReturn(Optional.of(new SamplingRule()));
        assertThatThrownBy(() -> service.create(request(3)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.SAMPLING_RULE_INVALID);
    }

    @Test
    void badSlotListRejected() {
        SamplingRuleCreateRequest request = request(3);
        request.setSlotList("1,a");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.SAMPLING_RULE_INVALID);
    }

    @Test
    void allLotsRuleIsSaved() {
        SamplingRuleCreateRequest request = request(0);
        request.setSampleType("ALL");
        request.setNValue(null);

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<SamplingRule>argThat(row -> "ALL".equals(row.getSampleType())));
    }

    @Test
    void percentRuleIsSaved() {
        SamplingRuleCreateRequest request = request(0);
        request.setSampleType("PERCENT");
        request.setPercent(new java.math.BigDecimal("20"));
        request.setNValue(null);

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<SamplingRule>argThat(row ->
                "PERCENT".equals(row.getSampleType()) && row.getPercent().compareTo(new java.math.BigDecimal("20")) == 0));
    }

    private SamplingRuleCreateRequest request(int n) {
        SamplingRuleCreateRequest request = new SamplingRuleCreateRequest();
        request.setRuleCode("SPC-3");
        request.setRuleName("每三批抽一批");
        request.setNValue(n);
        return request;
    }
}
