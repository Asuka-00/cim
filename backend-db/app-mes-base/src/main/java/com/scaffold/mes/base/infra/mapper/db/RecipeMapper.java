package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Recipe;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RecipeMapper Recipe Mapper
 * <p>
 * 映射 mes_recipe，并统计版本对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RecipeMapper extends BatchBaseMapper<Recipe> {

    /**
     * 统计版本和站点绑定对该 Recipe 的引用行数。
     *
     * @param id Recipe 主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_recipe_version WHERE recipe_id = #{id})
              + (SELECT COUNT(*) FROM mes_step_recipe WHERE recipe_id = #{id})
              + (SELECT COUNT(*) FROM mes_route_step_recipe WHERE recipe_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
