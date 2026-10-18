package com.scaffold.mes.exec.infra.repository.db;

import org.springframework.stereotype.Repository;

import com.scaffold.mes.exec.domain.entity.WoMaterialRequirement;
import com.scaffold.mes.exec.domain.repository.WoMaterialRequirementRepository;
import com.scaffold.mes.exec.infra.mapper.db.WoMaterialRequirementMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * WoMaterialRequirementDbRepositoryImpl 工单物料需求仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class WoMaterialRequirementDbRepositoryImpl implements WoMaterialRequirementRepository {

    private final WoMaterialRequirementMapper mapper;

    @Override
    public void save(WoMaterialRequirement requirement) {
        new DbRepositorySupport<>(mapper, WoMaterialRequirement.class).save(requirement);
    }
}
