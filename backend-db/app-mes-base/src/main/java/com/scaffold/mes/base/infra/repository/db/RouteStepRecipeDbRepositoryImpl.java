package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.RouteStepRecipe;
import com.scaffold.mes.base.domain.repository.RouteStepRecipeRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteStepRecipeMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepRecipeDbRepositoryImpl 步骤 Recipe 覆盖仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_route_step_recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteStepRecipeDbRepositoryImpl implements RouteStepRecipeRepository {

    private final RouteStepRecipeMapper mapper;

    private DbRepositorySupport<RouteStepRecipe> support() {
        return new DbRepositorySupport<>(mapper, RouteStepRecipe.class);
    }

    @Override
    public Optional<RouteStepRecipe> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<RouteStepRecipe> findExact(Long routeStepId, Long recipeId, Long productId) {
        return support().findOne(new LambdaQueryWrapper<RouteStepRecipe>()
                .eq(RouteStepRecipe::getRouteStepId, routeStepId)
                .eq(RouteStepRecipe::getRecipeId, recipeId)
                .eq(RouteStepRecipe::getProductId, productId));
    }

    @Override
    public List<RouteStepRecipe> listByVersion(Long routeVersionId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStepRecipe>()
                .eq(RouteStepRecipe::getRouteVersionId, routeVersionId));
    }

    @Override
    public List<RouteStepRecipe> listByStepAndProduct(Long routeStepId, Long productId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStepRecipe>()
                .eq(RouteStepRecipe::getRouteStepId, routeStepId)
                .eq(RouteStepRecipe::getProductId, productId));
    }

    @Override
    public void save(RouteStepRecipe binding) {
        support().save(binding);
    }

    @Override
    public void update(RouteStepRecipe binding) {
        support().update(binding);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
