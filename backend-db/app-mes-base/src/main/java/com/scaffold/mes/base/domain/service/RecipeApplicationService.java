package com.scaffold.mes.base.domain.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.Recipe;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * RecipeApplicationService Recipe 应用服务
 * <p>
 * 编码在租户内唯一。适用设备类型有值时必须是设备类型。已有版本时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RecipeApplicationService {

    private static final Set<String> EQUIPMENT_TYPES = Set.of(
            "PROCESS", "MEASURE", "TEST", "PACK", "TRANSPORT", "OTHER");

    private final RecipeRepository repository;

    /**
     * 分页查询 Recipe。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param recipeCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Recipe> page(int page, int pageSize, String recipeCode) {
        return repository.page(page, pageSize, recipeCode);
    }

    /**
     * 新增 Recipe。
     *
     * @param request Recipe 内容
     * @throws BusinessException 编码已存在，或设备类型不合法
     */
    @Transactional
    public void create(RecipeSaveRequest request) {
        if (repository.findByCode(request.getRecipeCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.RECIPE_CODE_EXISTS, "Recipe 编码已存在: " + request.getRecipeCode());
        }
        Recipe recipe = new Recipe();
        copy(request, recipe);
        repository.save(recipe);
    }

    /**
     * 修改 Recipe。
     *
     * @param id 主键
     * @param request Recipe 内容
     * @throws BusinessException Recipe 不存在、编码冲突，或设备类型不合法
     */
    @Transactional
    public void update(Long id, RecipeSaveRequest request) {
        Recipe recipe = require(id);
        repository.findByCode(request.getRecipeCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.RECIPE_CODE_EXISTS,
                            "Recipe 编码已存在: " + request.getRecipeCode());
                });
        copy(request, recipe);
        repository.update(recipe);
    }

    /**
     * 删除 Recipe。仍有版本时拒绝。
     *
     * @param id 主键
     * @throws BusinessException Recipe 不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.RECIPE_IN_USE, "Recipe",
                DeleteReferenceUtils.reference("版本", used));
        repository.deleteById(id);
    }

    private Recipe require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.RECIPE_NOT_FOUND, "Recipe 不存在"));
    }

    private static void copy(RecipeSaveRequest request, Recipe recipe) {
        if (request.getEquipmentType() != null && !request.getEquipmentType().isBlank()
                && !EQUIPMENT_TYPES.contains(request.getEquipmentType())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_TYPE_INVALID,
                    "设备类型不合法: " + request.getEquipmentType());
        }
        recipe.setRecipeCode(request.getRecipeCode());
        recipe.setRecipeName(request.getRecipeName());
        recipe.setEquipmentType(request.getEquipmentType() == null || request.getEquipmentType().isBlank()
                ? null
                : request.getEquipmentType());
        recipe.setDescription(request.getDescription());
        recipe.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
