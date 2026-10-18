package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.EquipmentCapability;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * EquipmentCapabilityMapper 设备能力 Mapper
 * <p>
 * 映射 mes_equipment_capability。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface EquipmentCapabilityMapper extends BatchBaseMapper<EquipmentCapability> {
}
