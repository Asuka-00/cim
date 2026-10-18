package com.scaffold.share.integration.mes.base.provider;

import com.scaffold.share.integration.mes.base.response.RouteStepStart;

/**
 * RouteStartProvider Route 起点查询
 * <p>
 * 由 app-mes-base 实现。开批时执行模块用它找到第一步，不直接读 Route 表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteStartProvider {

    /**
     * 取某个 Route 版本序号最小的步骤。
     *
     * @param routeVersionId Route 版本
     * @return 第一步
     */
    RouteStepStart first(Long routeVersionId);
}
