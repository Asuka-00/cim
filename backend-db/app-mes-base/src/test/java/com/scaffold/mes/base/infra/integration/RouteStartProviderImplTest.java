package com.scaffold.mes.base.infra.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.share.integration.mes.base.response.RouteStepStart;

/**
 * 开批停在序号最小的步骤，不按列表顺序取第一条。
 */
class RouteStartProviderImplTest {

    @Test
    void firstUsesSmallestSeq() {
        RouteStep later = step(30, 9L);
        RouteStep first = step(10, 7L);
        RouteStepRepository repository = mock(RouteStepRepository.class);
        when(repository.listByVersion(21L)).thenReturn(List.of(later, first));
        RouteStartProviderImpl provider = new RouteStartProviderImpl(repository);

        RouteStepStart start = provider.first(21L);

        assertThat(start.seqNo()).isEqualTo(10);
        assertThat(start.stepId()).isEqualTo(7L);
    }

    private static RouteStep step(int seq, Long stepId) {
        RouteStep step = new RouteStep();
        step.setSeqNo(seq);
        step.setStepId(stepId);
        return step;
    }
}
