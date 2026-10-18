package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.RecipeVersion;
import com.scaffold.mes.base.domain.repository.RecipeVersionRepository;
import com.scaffold.mes.base.infra.mapper.db.RecipeVersionMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RecipeVersionDbRepositoryImpl Recipe 版本仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_recipe_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RecipeVersionDbRepositoryImpl implements RecipeVersionRepository {

    private final RecipeVersionMapper mapper;

    private DbRepositorySupport<RecipeVersion> support() {
        return new DbRepositorySupport<>(mapper, RecipeVersion.class);
    }

    @Override
    public Optional<RecipeVersion> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public PageDTO<RecipeVersion> page(Long recipeId, int page, int pageSize) {
        return support().queryPage(page, pageSize, support().wrapper()
                .eq(RecipeVersion::getRecipeId, recipeId)
                .orderByDesc(RecipeVersion::getVersionNo));
    }

    @Override
    public List<RecipeVersion> listActive(Long recipeId) {
        return support().queryAll(support().wrapper()
                .eq(RecipeVersion::getRecipeId, recipeId)
                .eq(RecipeVersion::getStatus, "ACTIVE"));
    }

    @Override
    public int nextVersionNo(Long recipeId) {
        RecipeVersion latest = mapper.selectOne(support().wrapper()
                .eq(RecipeVersion::getRecipeId, recipeId)
                .orderByDesc(RecipeVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    @Override
    public void save(RecipeVersion version) {
        support().save(version);
    }

    @Override
    public void update(RecipeVersion version) {
        support().update(version);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
