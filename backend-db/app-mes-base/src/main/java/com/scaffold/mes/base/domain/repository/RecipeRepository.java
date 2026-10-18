package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Recipe;

/**
 * RecipeRepository Recipe 仓储
 * <p>
 * 按当前租户读写 mes_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RecipeRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return Recipe，不存在时为空
     */
    Optional<Recipe> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param recipeCode Recipe 编码
     * @return Recipe，不存在时为空
     */
    Optional<Recipe> findByCode(String recipeCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param recipeCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Recipe> page(int page, int pageSize, String recipeCode);

    /**
     * 插入。
     *
     * @param recipe Recipe
     */
    void save(Recipe recipe);

    /**
     * 更新。
     *
     * @param recipe 已加载的 Recipe
     */
    void update(Recipe recipe);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计版本引用数。
     *
     * @param id Recipe 主键
     * @return 引用条数
     */
    long countReferences(Long id);

    /**
     * 锁住 Recipe 主表行，供版本生效串行化。
     * <p>
     * 使用 {@code FOR UPDATE NOWAIT}。锁冲突时抛出 400102，与核心模块的对象占用码相同。
     *
     * @param id Recipe 主键
     * @throws com.scaffold.share.exception.BusinessException Recipe 不存在或行被占用
     */
    void lock(Long id);
}
