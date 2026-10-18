package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.StepEquipmentGroup;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StepEquipmentGroupMapper 站点设备组绑定 Mapper
 * <p>
 * 映射 mes_step_equipment_group。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StepEquipmentGroupMapper extends BatchBaseMapper<StepEquipmentGroup> {
}
