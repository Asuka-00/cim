package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.EquipmentGroup;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.infra.mapper.db.EquipmentGroupMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentGroupDbRepositoryImpl 设备组仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_equipment_group。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EquipmentGroupDbRepositoryImpl implements EquipmentGroupRepository {

    private final EquipmentGroupMapper mapper;

    private DbRepositorySupport<EquipmentGroup> support() {
        return new DbRepositorySupport<>(mapper, EquipmentGroup.class);
    }

    @Override
    public Optional<EquipmentGroup> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<EquipmentGroup> findByCode(String groupCode) {
        return support().findOne(new LambdaQueryWrapper<EquipmentGroup>().eq(EquipmentGroup::getGroupCode, groupCode));
    }

    @Override
    public PageDTO<EquipmentGroup> page(int page, int pageSize, String groupCode) {
        LambdaQueryWrapper<EquipmentGroup> wrapper = support().wrapper();
        if (groupCode != null && !groupCode.isBlank()) {
            wrapper.like(EquipmentGroup::getGroupCode, groupCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(EquipmentGroup group) {
        support().save(group);
    }

    @Override
    public void update(EquipmentGroup group) {
        support().update(group);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }
}
