package com.scaffold.mes.base.infra.repository.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.EquipmentCapability;
import com.scaffold.mes.base.domain.repository.EquipmentCapabilityRepository;
import com.scaffold.mes.base.infra.mapper.db.EquipmentCapabilityMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentCapabilityDbRepositoryImpl 设备能力仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_equipment_capability。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EquipmentCapabilityDbRepositoryImpl implements EquipmentCapabilityRepository {

    private final EquipmentCapabilityMapper mapper;

    private DbRepositorySupport<EquipmentCapability> support() {
        return new DbRepositorySupport<>(mapper, EquipmentCapability.class);
    }

    @Override
    public Optional<EquipmentCapability> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<EquipmentCapability> findExact(Long equipmentId, Long stepId, Long recipeId, Long productId) {
        return support().findOne(new LambdaQueryWrapper<EquipmentCapability>()
                .eq(EquipmentCapability::getEquipmentId, equipmentId)
                .eq(EquipmentCapability::getStepId, stepId)
                .eq(EquipmentCapability::getRecipeId, recipeId)
                .eq(EquipmentCapability::getProductId, productId));
    }

    @Override
    public List<EquipmentCapability> listByEquipment(Long equipmentId) {
        return mapper.selectList(new LambdaQueryWrapper<EquipmentCapability>().eq(EquipmentCapability::getEquipmentId, equipmentId));
    }

    @Override
    public List<EquipmentCapability> listByEquipmentAndStep(Long equipmentId, Long stepId) {
        return mapper.selectList(new LambdaQueryWrapper<EquipmentCapability>()
                .eq(EquipmentCapability::getEquipmentId, equipmentId)
                .eq(EquipmentCapability::getStepId, stepId));
    }

    @Override
    public List<EquipmentCapability> listDue(LocalDateTime now) {
        return mapper.selectList(new LambdaQueryWrapper<EquipmentCapability>()
                .eq(EquipmentCapability::getStatus, "VALID")
                .isNotNull(EquipmentCapability::getValidTo)
                .lt(EquipmentCapability::getValidTo, now));
    }

    @Override
    public void save(EquipmentCapability capability) {
        support().save(capability);
    }

    @Override
    public void update(EquipmentCapability capability) {
        support().update(capability);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
