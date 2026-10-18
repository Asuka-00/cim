package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.StepRecipe;

/**
 * StepRecipeRepository 站点 Recipe 绑定仓储
 * <p>
 * 按当前租户读写 mes_step_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepRecipeRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 绑定，不存在时为空
     */
    Optional<StepRecipe> findById(Long id);

    /**
     * 按站点、Recipe 和产品精确查询。
     *
     * @param stepId 站点
     * @param recipeId Recipe
     * @param productId 限定产品，0 表示通用
     * @return 绑定，不存在时为空
     */
    Optional<StepRecipe> findExact(Long stepId, Long recipeId, Long productId);

    /**
     * 列出某个站点在某个产品范围内的绑定。
     *
     * @param stepId 站点
     * @param productId 限定产品，0 表示通用
     * @return 绑定列表
     */
    List<StepRecipe> listByStepAndProduct(Long stepId, Long productId);

    /**
     * 列出某个站点的全部绑定，含限定产品和通用。
     *
     * @param stepId 站点
     * @return 绑定列表
     */
    List<StepRecipe> listByStep(Long stepId);

    /**
     * 插入。
     *
     * @param binding 绑定
     */
    void save(StepRecipe binding);

    /**
     * 更新。
     *
     * @param binding 已加载的绑定
     */
    void update(StepRecipe binding);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
