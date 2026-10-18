package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Route;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RouteMapper Route Mapper
 * <p>
 * 映射 mes_route，并统计版本对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RouteMapper extends BatchBaseMapper<Route> {

    /**
     * 统计仍引用该 Route 的版本行数。
     *
     * @param id Route 主键
     * @return 引用条数
     */
    @Select("SELECT COUNT(*) FROM mes_route_version WHERE route_id = #{id}")
    long countReferences(@Param("id") Long id);
}
