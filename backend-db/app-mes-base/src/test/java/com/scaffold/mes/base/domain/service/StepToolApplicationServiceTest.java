package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepTool;
import com.scaffold.mes.base.domain.entity.StepTool;
import com.scaffold.mes.base.domain.entity.Tool;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteStepToolRepository;
import com.scaffold.mes.base.domain.repository.StepToolRepository;
import com.scaffold.mes.base.domain.repository.ToolRepository;

/**
 * 没有要求时不限制。产品专用要求优先于通用要求。
 */
class StepToolApplicationServiceTest {

    private StepToolRepository requirements;
    private RouteStepRepository routeSteps;
    private RouteStepToolRepository routeTools;
    private ToolRepository tools;
    private StepToolApplicationService service;

    @BeforeEach
    void setUp() {
        requirements = mock(StepToolRepository.class);
        RouteStepRepository routeSteps = mock(RouteStepRepository.class);
        routeTools = mock(RouteStepToolRepository.class);
        tools = mock(ToolRepository.class);
        this.routeSteps = routeSteps;
        service = new StepToolApplicationService(requirements, routeSteps, routeTools, tools);
    }

    @Test
    void noRequirementAllowsMissingTool() {
        when(requirements.listByStep(7L)).thenReturn(List.of());

        assertThat(service.accepted(7L, 1L, null, null, null)).isTrue();
    }

    @Test
    void productRequirementRejectsOtherType() {
        when(requirements.listByStep(7L)).thenReturn(List.of(row(1L, 11L), row(0L, 22L)));
        Tool tool = new Tool();
        tool.setTypeId(22L);
        when(tools.findById(4L)).thenReturn(Optional.of(tool));

        assertThat(service.accepted(7L, 1L, 4L, null, null)).isFalse();
        assertThat(service.accepted(7L, 9L, 4L, null, null)).isTrue();
    }

    @Test
    void routeStepReplacesStationRequirement() {
        when(requirements.listByStep(7L)).thenReturn(List.of(row(1L, 11L)));
        RouteStep step = new RouteStep();
        step.setId(30L);
        when(routeSteps.findBySeq(21L, 10)).thenReturn(Optional.of(step));
        RouteStepTool override = new RouteStepTool();
        override.setProductId(1L);
        override.setToolTypeId(33L);
        when(routeTools.listByRouteStep(30L)).thenReturn(List.of(override));
        Tool tool = new Tool();
        tool.setTypeId(11L);
        when(tools.findById(4L)).thenReturn(Optional.of(tool));

        assertThat(service.accepted(7L, 1L, 4L, 21L, 10)).isFalse();
    }

    private static StepTool row(Long productId, Long typeId) {
        StepTool row = new StepTool();
        row.setProductId(productId);
        row.setToolTypeId(typeId);
        return row;
    }
}
