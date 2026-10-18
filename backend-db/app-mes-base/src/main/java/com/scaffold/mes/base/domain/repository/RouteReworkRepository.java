package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.scaffold.mes.base.domain.entity.RouteRework;

/**
 * RouteReworkRepository 返工分支仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteReworkRepository {

    /**
     * 按主键查询。
     *
     * @param id 返工分支
     * @return 分支，不存在时为空
     */
    Optional<RouteRework> findById(Long id);
}
