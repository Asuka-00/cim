package com.scaffold.mes.exec.domain.repository;

import com.scaffold.mes.exec.domain.entity.WoMaterialRequirement;

/**
 * WoMaterialRequirementRepository 工单物料需求仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface WoMaterialRequirementRepository {

    /**
     * 插入一行需求。
     *
     * @param requirement 需求
     */
    void save(WoMaterialRequirement requirement);
}
