package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.infra.mapper.db.EquipmentMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentDbRepositoryImpl 设备台账仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_equipment。更新走乐观锁。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EquipmentDbRepositoryImpl implements EquipmentRepository {

    private final EquipmentMapper mapper;

    private DbRepositorySupport<Equipment> support() {
        return new DbRepositorySupport<>(mapper, Equipment.class);
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Equipment> findByCode(String equipmentCode) {
        return support().findOne(new LambdaQueryWrapper<Equipment>().eq(Equipment::getEquipmentCode, equipmentCode));
    }

    @Override
    public PageDTO<Equipment> page(int page, int pageSize, String equipmentCode) {
        LambdaQueryWrapper<Equipment> wrapper = support().wrapper();
        if (equipmentCode != null && !equipmentCode.isBlank()) {
            wrapper.like(Equipment::getEquipmentCode, equipmentCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Equipment equipment) {
        support().save(equipment);
    }

    @Override
    public void update(Equipment equipment) {
        support().updateWithVersion(equipment);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countByE10State(String e10State) {
        Long count = mapper.selectCount(new LambdaQueryWrapper<Equipment>().eq(Equipment::getE10State, e10State));
        return count == null ? 0 : count;
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }
}
