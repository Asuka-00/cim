package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.UnitSerial;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * UnitSerialMapper Unit 序列号 Mapper
 * <p>
 * 映射 mes_unit_serial。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface UnitSerialMapper extends BatchBaseMapper<UnitSerial> {
}
