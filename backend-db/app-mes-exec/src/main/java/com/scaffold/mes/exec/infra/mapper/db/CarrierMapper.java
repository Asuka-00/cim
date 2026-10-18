package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.Carrier;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * CarrierMapper 载具 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface CarrierMapper extends BatchBaseMapper<Carrier> {
}
