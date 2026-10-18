package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepCollectItemSaveRequest;
import com.scaffold.mes.base.domain.entity.StepCollectItem;
import com.scaffold.mes.base.domain.repository.StepCollectItemRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 站点采集规格至少要有上限或下限。同一站点、采集项和产品不能重复。
 */
class StepCollectItemApplicationServiceTest {

    private StepCollectItemRepository repository;
    private StepCollectItemApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StepCollectItemRepository.class);
        when(repository.find(7L, 15L, 0L)).thenReturn(Optional.empty());
        service = new StepCollectItemApplicationService(repository);
    }

    @Test
    void limitsAreSaved() {
        StepCollectItemSaveRequest request = new StepCollectItemSaveRequest();
        request.setStepId(7L);
        request.setItemId(15L);
        request.setLsl(new BigDecimal("1"));
        request.setUsl(new BigDecimal("2"));

        service.save(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<StepCollectItem>argThat(row ->
                row.getProductId().equals(0L) && row.getLsl().compareTo(new BigDecimal("1")) == 0));
    }

    @Test
    void bothLimitsMissingRejected() {
        StepCollectItemSaveRequest request = new StepCollectItemSaveRequest();
        request.setStepId(7L);
        request.setItemId(15L);

        assertThatThrownBy(() -> service.save(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_COLLECT_INVALID);
    }

    @Test
    void autoHoldNeedsReason() {
        StepCollectItemSaveRequest request = new StepCollectItemSaveRequest();
        request.setStepId(7L);
        request.setItemId(15L);
        request.setUsl(new BigDecimal("2"));
        request.setAutoHoldOnOos(1);

        assertThatThrownBy(() -> service.save(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_COLLECT_INVALID);
    }
}
