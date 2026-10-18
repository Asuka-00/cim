package com.scaffold.mes.warehouse.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.warehouse.domain.entity.IncomingInspection;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * IncomingInspectionMapper 来料检验 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface IncomingInspectionMapper extends BatchBaseMapper<IncomingInspection> {
}
