package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.RecipeVersion;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RecipeVersionMapper Recipe 版本 Mapper
 * <p>
 * 映射 mes_recipe_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RecipeVersionMapper extends BatchBaseMapper<RecipeVersion> {
}
