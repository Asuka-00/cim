package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StepRecipeMapper 站点 Recipe 绑定 Mapper
 * <p>
 * 映射 mes_step_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StepRecipeMapper extends BatchBaseMapper<StepRecipe> {
}
