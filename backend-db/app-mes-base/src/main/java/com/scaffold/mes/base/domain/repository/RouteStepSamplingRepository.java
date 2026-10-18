package com.scaffold.mes.base.domain.repository;

import java.util.List;

import com.scaffold.mes.base.domain.entity.RouteStepSampling;

/**
 * RouteStepSamplingRepository Route 步骤抽检覆盖仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteStepSamplingRepository {

    /**
     * 列出这个 Route 步骤的覆盖。
     *
     * @param routeStepId Route 步骤
     * @return 覆盖，没有时为空列表
     */
    List<RouteStepSampling> listByRouteStep(Long routeStepId);

    /**
     * 列出这个版本的抽检覆盖。
     *
     * @param routeVersionId Route 版本
     * @return 覆盖，没有时为空列表
     */
    List<RouteStepSampling> listByVersion(Long routeVersionId);

    /**
     * 插入。
     *
     * @param sampling 覆盖
     */
    void save(RouteStepSampling sampling);
}
