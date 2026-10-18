package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.RecipeVersion;

/**
 * RecipeVersionRepository Recipe 版本仓储
 * <p>
 * 按当前租户读写 mes_recipe_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RecipeVersionRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 版本，不存在时为空
     */
    Optional<RecipeVersion> findById(Long id);

    /**
     * 分页查询某个 Recipe 的版本。
     *
     * @param recipeId Recipe
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 按版本号倒序的分页结果
     */
    PageDTO<RecipeVersion> page(Long recipeId, int page, int pageSize);

    /**
     * 列出某个 Recipe 当前生效的版本。
     *
     * @param recipeId Recipe
     * @return 状态为 ACTIVE 的版本
     */
    List<RecipeVersion> listActive(Long recipeId);

    /**
     * 计算下一个版本号。
     *
     * @param recipeId Recipe
     * @return 现有最大版本号加 1，没有版本时为 1
     */
    int nextVersionNo(Long recipeId);

    /**
     * 插入。
     *
     * @param version 版本
     */
    void save(RecipeVersion version);

    /**
     * 更新。
     *
     * @param version 已加载的版本
     */
    void update(RecipeVersion version);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
