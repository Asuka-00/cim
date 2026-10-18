package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.mes.base.domain.repository.StepRecipeRepository;
import com.scaffold.mes.base.infra.mapper.db.StepRecipeMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StepRecipeDbRepositoryImpl 站点 Recipe 绑定仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_step_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepRecipeDbRepositoryImpl implements StepRecipeRepository {

    private final StepRecipeMapper mapper;

    private DbRepositorySupport<StepRecipe> support() {
        return new DbRepositorySupport<>(mapper, StepRecipe.class);
    }

    @Override
    public Optional<StepRecipe> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<StepRecipe> findExact(Long stepId, Long recipeId, Long productId) {
        return support().findOne(new LambdaQueryWrapper<StepRecipe>()
                .eq(StepRecipe::getStepId, stepId)
                .eq(StepRecipe::getRecipeId, recipeId)
                .eq(StepRecipe::getProductId, productId));
    }

    @Override
    public List<StepRecipe> listByStep(Long stepId) {
        return mapper.selectList(new LambdaQueryWrapper<StepRecipe>().eq(StepRecipe::getStepId, stepId));
    }

    @Override
    public List<StepRecipe> listByStepAndProduct(Long stepId, Long productId) {
        return mapper.selectList(new LambdaQueryWrapper<StepRecipe>()
                .eq(StepRecipe::getStepId, stepId)
                .eq(StepRecipe::getProductId, productId));
    }

    @Override
    public void save(StepRecipe binding) {
        support().save(binding);
    }

    @Override
    public void update(StepRecipe binding) {
        support().update(binding);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
