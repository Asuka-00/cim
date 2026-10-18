package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.StepEquipmentGroup;
import com.scaffold.mes.base.domain.repository.StepEquipmentGroupRepository;
import com.scaffold.mes.base.infra.mapper.db.StepEquipmentGroupMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StepEquipmentGroupDbRepositoryImpl 站点设备组绑定仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_step_equipment_group。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepEquipmentGroupDbRepositoryImpl implements StepEquipmentGroupRepository {

    private final StepEquipmentGroupMapper mapper;

    private DbRepositorySupport<StepEquipmentGroup> support() {
        return new DbRepositorySupport<>(mapper, StepEquipmentGroup.class);
    }

    @Override
    public Optional<StepEquipmentGroup> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<StepEquipmentGroup> findExact(Long stepId, Long groupId) {
        return support().findOne(new LambdaQueryWrapper<StepEquipmentGroup>()
                .eq(StepEquipmentGroup::getStepId, stepId)
                .eq(StepEquipmentGroup::getGroupId, groupId));
    }

    @Override
    public List<StepEquipmentGroup> listByStep(Long stepId) {
        return mapper.selectList(new LambdaQueryWrapper<StepEquipmentGroup>()
                .eq(StepEquipmentGroup::getStepId, stepId)
                .orderByAsc(StepEquipmentGroup::getPriority));
    }

    @Override
    public List<StepEquipmentGroup> listByGroup(Long groupId) {
        return mapper.selectList(new LambdaQueryWrapper<StepEquipmentGroup>().eq(StepEquipmentGroup::getGroupId, groupId));
    }

    @Override
    public void save(StepEquipmentGroup binding) {
        support().save(binding);
    }

    @Override
    public void update(StepEquipmentGroup binding) {
        support().update(binding);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
