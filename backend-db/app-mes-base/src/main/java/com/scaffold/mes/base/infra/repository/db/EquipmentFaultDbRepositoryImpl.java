package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.EquipmentFault;
import com.scaffold.mes.base.domain.entity.EquipmentFaultLot;
import com.scaffold.mes.base.domain.repository.EquipmentFaultRepository;
import com.scaffold.mes.base.infra.mapper.db.EquipmentFaultLotMapper;
import com.scaffold.mes.base.infra.mapper.db.EquipmentFaultMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentFaultDbRepositoryImpl 设备异常仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EquipmentFaultDbRepositoryImpl implements EquipmentFaultRepository {

    private final EquipmentFaultMapper faults;
    private final EquipmentFaultLotMapper lots;

    @Override
    public Optional<EquipmentFault> findByNo(String faultNo) {
        return new DbRepositorySupport<>(faults, EquipmentFault.class)
                .findOne(new LambdaQueryWrapper<EquipmentFault>().eq(EquipmentFault::getFaultNo, faultNo));
    }

    @Override
    public Optional<EquipmentFault> findById(Long id) {
        return new DbRepositorySupport<>(faults, EquipmentFault.class).findById(id);
    }

    @Override
    public List<EquipmentFault> list() {
        return faults.selectList(new LambdaQueryWrapper<EquipmentFault>().orderByAsc(EquipmentFault::getFaultNo));
    }

    @Override
    public void save(EquipmentFault fault) {
        new DbRepositorySupport<>(faults, EquipmentFault.class).save(fault);
    }

    @Override
    public void update(EquipmentFault fault) {
        new DbRepositorySupport<>(faults, EquipmentFault.class).update(fault);
    }

    @Override
    public void saveLot(EquipmentFaultLot link) {
        new DbRepositorySupport<>(lots, EquipmentFaultLot.class).save(link);
    }
}
