package com.scaffold.mes.warehouse.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.warehouse.domain.entity.IncomingWafer;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * IncomingWaferMapper 来料 Wafer Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface IncomingWaferMapper extends BatchBaseMapper<IncomingWafer> {
}
