package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * EquipmentPortMapper 设备端口 Mapper
 * <p>
 * 映射 mes_equipment_port。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface EquipmentPortMapper extends BatchBaseMapper<EquipmentPort> {
}
