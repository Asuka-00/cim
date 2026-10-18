package com.scaffold.mes.base.infra.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.repository.RouteStepCollectItemRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.StepCollectItemRepository;
import com.scaffold.share.integration.mes.base.response.CollectLimit;

/**
 * 步骤上有采集规格时，不再使用站点规格。
 */
class CollectSpecPortImplTest {

    private StepCollectItemRepository items;
    private RouteStepRepository routeSteps;
    private RouteStepCollectItemRepository routeItems;
    private CollectSpecPortImpl port;

    @BeforeEach
    void setUp() {
        items = mock(StepCollectItemRepository.class);
        routeSteps = mock(RouteStepRepository.class);
        routeItems = mock(RouteStepCollectItemRepository.class);
        when(routeItems.listByRouteStep(30L)).thenReturn(List.of());
        port = new CollectSpecPortImpl(items, routeSteps, routeItems);
    }

    @Test
    void routeLimitReplacesStation() {
        RouteStep step = new RouteStep();
        step.setId(30L);
        when(routeSteps.findBySeq(21L, 20)).thenReturn(Optional.of(step));
        RouteStepCollectItem row = new RouteStepCollectItem();
        row.setItemId(15L);
        row.setProductId(0L);
        row.setLsl(new BigDecimal("9"));
        row.setUsl(new BigDecimal("10"));
        when(routeItems.listByRouteStep(30L)).thenReturn(List.of(row));

        Optional<CollectLimit> limit = port.limit(21L, 20, 7L, 15L, null);

        assertThat(limit).isPresent();
        assertThat(limit.orElseThrow().lsl()).isEqualByComparingTo("9");
        verify(items, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void routeRequiredIgnoresStationItems() {
        RouteStep step = new RouteStep();
        step.setId(30L);
        when(routeSteps.findBySeq(21L, 20)).thenReturn(Optional.of(step));
        RouteStepCollectItem row = new RouteStepCollectItem();
        row.setItemId(16L);
        row.setProductId(0L);
        row.setIsRequired(1);
        when(routeItems.listByRouteStep(30L)).thenReturn(List.of(row));

        assertThat(port.requiredItems(21L, 20, 7L, null)).containsExactly(16L);
        verify(items, never()).listByStep(7L);
    }
}
