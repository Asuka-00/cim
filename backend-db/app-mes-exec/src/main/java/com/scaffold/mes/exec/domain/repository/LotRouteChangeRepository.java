package com.scaffold.mes.exec.domain.repository;

import com.scaffold.mes.exec.domain.entity.LotRouteChange;

/**
 * LotRouteChangeRepository 工艺变更仓储
 * <p>
 * 只追加 hist_mes_lot_route_change。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotRouteChangeRepository {

    /**
     * 插入一条变更记录。
     *
     * @param change 变更
     */
    void save(LotRouteChange change);
}
