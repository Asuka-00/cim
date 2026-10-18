package com.scaffold.mes.base.infra.repository.db;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.Recipe;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.infra.mapper.db.RecipeMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RecipeDbRepositoryImpl Recipe 仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RecipeDbRepositoryImpl implements RecipeRepository {

    private final RecipeMapper mapper;

    private DbRepositorySupport<Recipe> support() {
        return new DbRepositorySupport<>(mapper, Recipe.class);
    }

    @Override
    public Optional<Recipe> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Recipe> findByCode(String recipeCode) {
        return support().findOne(new LambdaQueryWrapper<Recipe>().eq(Recipe::getRecipeCode, recipeCode));
    }

    @Override
    public PageDTO<Recipe> page(int page, int pageSize, String recipeCode) {
        LambdaQueryWrapper<Recipe> wrapper = support().wrapper();
        if (recipeCode != null && !recipeCode.isBlank()) {
            wrapper.like(Recipe::getRecipeCode, recipeCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Recipe recipe) {
        support().save(recipe);
    }

    @Override
    public void update(Recipe recipe) {
        support().update(recipe);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }

    @Override
    public void lock(Long id) {
        try {
            Recipe locked = mapper.selectOne(new LambdaQueryWrapper<Recipe>()
                    .eq(Recipe::getId, id)
                    .last("FOR UPDATE NOWAIT"));
            if (locked == null) {
                throw new BusinessException(MesBaseErrorCodes.RECIPE_NOT_FOUND, "Recipe 不存在");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            BusinessException busy = lockConflict(ex);
            if (busy != null) {
                throw busy;
            }
            throw ex;
        }
    }

    /**
     * 翻译 MySQL 行锁错误。3572 是 NOWAIT 立即失败，1205 是等待超时。
     * 码值与核心模块一致，主数据模块不引用核心包。
     *
     * @param error 数据访问异常
     * @return 业务异常，不是锁冲突时为空
     */
    private static BusinessException lockConflict(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sql) {
                if (sql.getErrorCode() == 3572) {
                    return new BusinessException(400102, "对象被占用");
                }
                if (sql.getErrorCode() == 1205) {
                    return new BusinessException(400103, "锁等待超时");
                }
            }
            current = current.getCause();
        }
        return null;
    }
}
