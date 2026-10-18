package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteStepCollectItemSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.repository.RouteStepCollectItemRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Route 步骤采集规格至少要有上限或下限。同一采集项与产品不能重复。
 */
class RouteStepCollectItemApplicationServiceTest {

    private RouteStepCollectItemRepository items;
    private RouteStepCollectItemApplicationService service;

    @BeforeEach
    void setUp() {
        RouteStepRepository routeSteps = mock(RouteStepRepository.class);
        items = mock(RouteStepCollectItemRepository.class);
        RouteStep step = new RouteStep();
        step.setId(30L);
        when(routeSteps.findBySeq(21L, 20)).thenReturn(Optional.of(step));
        when(items.listByRouteStep(30L)).thenReturn(List.of());
        service = new RouteStepCollectItemApplicationService(routeSteps, items);
    }

    @Test
    void savesRequiredLimit() {
        RouteStepCollectItemSaveRequest request = request();
        request.setLsl(new BigDecimal("1"));
        request.setUsl(new BigDecimal("2"));

        service.save(request);

        verify(items).save(org.mockito.ArgumentMatchers.<RouteStepCollectItem>argThat(row ->
                row.getRouteStepId().equals(30L) && row.getItemId().equals(15L) && row.getProductId().equals(0L)
                        && row.getIsRequired().equals(1)));
    }

    @Test
    void bothLimitsMissingRejected() {
        assertThatThrownBy(() -> service.save(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_COLLECT_INVALID);
    }

    @Test
    void duplicateItemRejected() {
        RouteStepCollectItem existing = new RouteStepCollectItem();
        existing.setItemId(15L);
        existing.setProductId(0L);
        when(items.listByRouteStep(30L)).thenReturn(List.of(existing));
        RouteStepCollectItemSaveRequest request = request();
        request.setUsl(new BigDecimal("2"));

        assertThatThrownBy(() -> service.save(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_COLLECT_INVALID);
    }

    private static RouteStepCollectItemSaveRequest request() {
        RouteStepCollectItemSaveRequest request = new RouteStepCollectItemSaveRequest();
        request.setRouteVersionId(21L);
        request.setSeqNo(20);
        request.setItemId(15L);
        return request;
    }
}
