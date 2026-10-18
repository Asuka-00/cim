package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.InterlockLog;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * InterlockLogMapper 进站阻断日志 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface InterlockLogMapper extends BatchBaseMapper<InterlockLog> {
}
