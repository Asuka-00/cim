package com.scaffold.mes.core.infra.mapper.db.shift;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.shift.Shift;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ShiftMapper 班次 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ShiftMapper extends BatchBaseMapper<Shift> {
}
