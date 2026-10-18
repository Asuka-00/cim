package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.RouteStepRecipe;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RouteStepRecipeMapper 步骤 Recipe 覆盖 Mapper
 * <p>
 * 映射 mes_route_step_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RouteStepRecipeMapper extends BatchBaseMapper<RouteStepRecipe> {
}
