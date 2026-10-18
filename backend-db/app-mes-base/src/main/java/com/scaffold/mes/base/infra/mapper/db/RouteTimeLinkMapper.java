package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.RouteTimeLink;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RouteTimeLinkMapper Time Link Mapper
 * <p>
 * 映射 mes_route_time_link。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RouteTimeLinkMapper extends BatchBaseMapper<RouteTimeLink> {
}
