package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteSaveRequest;
import com.scaffold.mes.base.domain.entity.Route;
import com.scaffold.mes.base.domain.repository.RouteRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Route 编码不能重复，已有版本时不能删除。
 */
class RouteApplicationServiceTest {

    private RouteRepository repository;
    private RouteApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(RouteRepository.class);
        service = new RouteApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("FEOL")).thenReturn(Optional.of(new Route()));

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRejectsWhenVersionExists() {
        Route route = new Route();
        route.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(route));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        Route route = new Route();
        route.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(route));
        when(repository.countReferences(1L)).thenReturn(0L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private static RouteSaveRequest request() {
        RouteSaveRequest request = new RouteSaveRequest();
        request.setRouteCode("FEOL");
        request.setRouteName("前道");
        return request;
    }
}
