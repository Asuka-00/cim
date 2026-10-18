package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.scaffold.mes.base.domain.entity.StepInstruction;

/**
 * StepInstructionRepository 站点作业指导仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepInstructionRepository {

    /**
     * 按站点和产品查询。
     *
     * @param stepId 站点
     * @param productId 产品。0 表示通用
     * @return 指导，不存在时为空
     */
    Optional<StepInstruction> find(Long stepId, Long productId);

    /**
     * 插入。
     *
     * @param instruction 指导
     */
    void save(StepInstruction instruction);
}
