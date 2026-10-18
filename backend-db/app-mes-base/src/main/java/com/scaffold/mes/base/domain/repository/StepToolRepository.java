package com.scaffold.mes.base.domain.repository;

import java.util.List;

import com.scaffold.mes.base.domain.entity.StepTool;

/**
 * StepToolRepository 站点工具要求仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepToolRepository {

    /**
     * 列出站点上的全部要求。
     *
     * @param stepId 站点
     * @return 要求，没有时为空列表
     */
    List<StepTool> listByStep(Long stepId);
}
