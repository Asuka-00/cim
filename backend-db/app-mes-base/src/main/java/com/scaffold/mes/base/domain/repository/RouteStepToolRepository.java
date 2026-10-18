package com.scaffold.mes.base.domain.repository;

import java.util.List;

import com.scaffold.mes.base.domain.entity.RouteStepTool;

/**
 * RouteStepToolRepository Route 步骤工具要求仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteStepToolRepository {

    /**
     * 列出某个 Route 步骤上的要求。
     *
     * @param routeStepId Route 步骤
     * @return 要求，没有时为空列表
     */
    List<RouteStepTool> listByRouteStep(Long routeStepId);
}
