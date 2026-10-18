package com.scaffold.mes.base.domain.repository;

import java.util.List;

import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;

/**
 * RouteStepCollectItemRepository Route 步骤采集规格仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteStepCollectItemRepository {

    /**
     * 列出这个 Route 步骤的规格。
     *
     * @param routeStepId Route 步骤
     * @return 规格，没有时为空列表
     */
    List<RouteStepCollectItem> listByRouteStep(Long routeStepId);

    /**
     * 列出这个版本的采集规格。
     *
     * @param routeVersionId Route 版本
     * @return 规格，没有时为空列表
     */
    List<RouteStepCollectItem> listByVersion(Long routeVersionId);

    /**
     * 插入。
     *
     * @param item 规格
     */
    void save(RouteStepCollectItem item);
}
