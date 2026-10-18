package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RouteStepMapper Route 步骤 Mapper
 * <p>
 * 映射 mes_route_step。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RouteStepMapper extends BatchBaseMapper<RouteStep> {

    /**
     * 统计仍覆盖该步骤的 Recipe 行数。
     *
     * @param id Route 步骤主键
     * @return 引用条数
     */
    @Select("SELECT COUNT(*) FROM mes_route_step_recipe WHERE route_step_id = #{id}")
    long countReferences(@Param("id") Long id);
}
