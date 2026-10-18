package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.EquipmentFaultLot;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * EquipmentFaultLotMapper 异常影响 Lot Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface EquipmentFaultLotMapper extends BatchBaseMapper<EquipmentFaultLot> {
}
