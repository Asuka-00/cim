package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.mes.base.domain.repository.EquipmentPortRepository;
import com.scaffold.mes.base.infra.mapper.db.EquipmentPortMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentPortDbRepositoryImpl 设备端口仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_equipment_port。更新走乐观锁。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EquipmentPortDbRepositoryImpl implements EquipmentPortRepository {

    private final EquipmentPortMapper mapper;

    private DbRepositorySupport<EquipmentPort> support() {
        return new DbRepositorySupport<>(mapper, EquipmentPort.class);
    }

    @Override
    public Optional<EquipmentPort> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<EquipmentPort> findByCode(Long equipmentId, String portCode) {
        return support().findOne(new LambdaQueryWrapper<EquipmentPort>()
                .eq(EquipmentPort::getEquipmentId, equipmentId)
                .eq(EquipmentPort::getPortCode, portCode));
    }

    @Override
    public List<EquipmentPort> listByEquipment(Long equipmentId) {
        return mapper.selectList(new LambdaQueryWrapper<EquipmentPort>()
                .eq(EquipmentPort::getEquipmentId, equipmentId)
                .orderByAsc(EquipmentPort::getPortCode));
    }

    @Override
    public void save(EquipmentPort port) {
        support().save(port);
    }

    @Override
    public void update(EquipmentPort port) {
        support().updateWithVersion(port);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
