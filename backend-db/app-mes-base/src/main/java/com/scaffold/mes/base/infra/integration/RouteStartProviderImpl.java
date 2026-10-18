package com.scaffold.mes.base.infra.integration;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteStartProvider;
import com.scaffold.share.integration.mes.base.response.RouteStepStart;

import lombok.RequiredArgsConstructor;

/**
 * RouteStartProviderImpl Route 起点查询实现
 * <p>
 * 开批停在序号最小的步骤。没有步骤时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class RouteStartProviderImpl implements RouteStartProvider {

    private final RouteStepRepository routeStepRepository;

    @Override
    public RouteStepStart first(Long routeVersionId) {
        return routeStepRepository.listByVersion(routeVersionId).stream()
                .min(Comparator.comparing(RouteStep::getSeqNo, Comparator.nullsLast(Integer::compareTo)))
                .map(step -> new RouteStepStart(step.getSeqNo(), step.getStepId()))
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_STEP_NOT_FOUND, "Route 还没有步骤"));
    }
}
