package com.scaffold.mes.base.domain.repository;

import java.util.List;

import com.scaffold.mes.base.domain.entity.RouteReworkStep;

/**
 * RouteReworkStepRepository 返工步骤仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteReworkStepRepository {

    /**
     * 列出某分支的步骤。
     *
     * @param reworkId 返工分支
     * @return 步骤
     */
    List<RouteReworkStep> listByRework(Long reworkId);
}
