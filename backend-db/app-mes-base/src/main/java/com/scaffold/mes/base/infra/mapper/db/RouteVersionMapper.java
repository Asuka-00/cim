package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RouteVersionMapper Route 版本 Mapper
 * <p>
 * 映射 mes_route_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RouteVersionMapper extends BatchBaseMapper<RouteVersion> {
}
