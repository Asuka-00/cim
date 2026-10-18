package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.RouteStepRecipe;

/**
 * RouteStepRecipeRepository 步骤 Recipe 覆盖仓储
 * <p>
 * 按当前租户读写 mes_route_step_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteStepRecipeRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 覆盖，不存在时为空
     */
    Optional<RouteStepRecipe> findById(Long id);

    /**
     * 按步骤、Recipe 和产品精确查询。
     *
     * @param routeStepId Route 步骤
     * @param recipeId Recipe
     * @param productId 限定产品，0 表示通用
     * @return 覆盖，不存在时为空
     */
    Optional<RouteStepRecipe> findExact(Long routeStepId, Long recipeId, Long productId);

    /**
     * 列出某个版本的全部步骤 Recipe 覆盖。
     *
     * @param routeVersionId Route 版本
     * @return 覆盖列表
     */
    List<RouteStepRecipe> listByVersion(Long routeVersionId);

    /**
     * 列出某个步骤在某个产品范围内的覆盖。
     *
     * @param routeStepId Route 步骤
     * @param productId 限定产品，0 表示通用
     * @return 覆盖列表
     */
    List<RouteStepRecipe> listByStepAndProduct(Long routeStepId, Long productId);

    /**
     * 插入。
     *
     * @param binding 覆盖
     */
    void save(RouteStepRecipe binding);

    /**
     * 更新。
     *
     * @param binding 已加载的覆盖
     */
    void update(RouteStepRecipe binding);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
