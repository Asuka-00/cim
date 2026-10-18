package com.scaffold.mes.base.infra.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;

/**
 * 出站取比当前序号更大的最小步骤。
 */
class RouteNextProviderImplTest {

    @Test
    void nextSkipsCurrentAndEarlier() {
        RouteStepRepository routes = mock(RouteStepRepository.class);
        StepRepository steps = mock(StepRepository.class);
        when(routes.listByVersion(21L)).thenReturn(List.of(route(10, 7L), route(30, 8L), route(20, 9L)));
        Step area = new Step();
        area.setAreaId(4L);
        when(steps.findById(9L)).thenReturn(Optional.of(area));
        RouteNextProviderImpl provider = new RouteNextProviderImpl(routes, steps);

        RouteStepNext next = provider.next(21L, 10).orElseThrow();

        assertThat(next.seqNo()).isEqualTo(20);
        assertThat(next.stepId()).isEqualTo(9L);
        assertThat(next.areaId()).isEqualTo(4L);
    }

    private static RouteStep route(int seq, Long stepId) {
        RouteStep step = new RouteStep();
        step.setSeqNo(seq);
        step.setStepId(stepId);
        return step;
    }
}
