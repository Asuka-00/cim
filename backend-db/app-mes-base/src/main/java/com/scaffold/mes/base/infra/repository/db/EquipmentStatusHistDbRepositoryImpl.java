package com.scaffold.mes.base.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.EquipmentStatusHist;
import com.scaffold.mes.base.domain.repository.EquipmentStatusHistRepository;
import com.scaffold.mes.base.infra.mapper.db.EquipmentStatusHistMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentStatusHistDbRepositoryImpl 设备状态历史仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 hist_mes_equipment_status。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class EquipmentStatusHistDbRepositoryImpl implements EquipmentStatusHistRepository {

    private final EquipmentStatusHistMapper mapper;

    private DbRepositorySupport<EquipmentStatusHist> support() {
        return new DbRepositorySupport<>(mapper, EquipmentStatusHist.class);
    }

    @Override
    public List<EquipmentStatusHist> listOpen(Long equipmentId, Long portId) {
        return support().queryAll(support().wrapper()
                .eq(EquipmentStatusHist::getEquipmentId, equipmentId)
                .eq(EquipmentStatusHist::getPortId, portId)
                .isNull(EquipmentStatusHist::getEndAt)
                .orderByAsc(EquipmentStatusHist::getStartAt));
    }

    @Override
    public PageDTO<EquipmentStatusHist> page(Long equipmentId, int page, int pageSize) {
        return support().queryPage(page, pageSize, support().wrapper()
                .eq(EquipmentStatusHist::getEquipmentId, equipmentId)
                .orderByDesc(EquipmentStatusHist::getStartAt));
    }

    @Override
    public void save(EquipmentStatusHist hist) {
        support().save(hist);
    }

    @Override
    public void update(EquipmentStatusHist hist) {
        support().update(hist);
    }

    @Override
    public void deleteUntouchedInitial(Long equipmentId) {
        mapper.deleteUntouchedInitial(equipmentId);
    }
}
