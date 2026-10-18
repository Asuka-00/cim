package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.RouteRework;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RouteReworkMapper 返工分支 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RouteReworkMapper extends BatchBaseMapper<RouteRework> {
}
