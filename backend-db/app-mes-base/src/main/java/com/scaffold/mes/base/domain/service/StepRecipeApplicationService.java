package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepRecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.domain.repository.StepRecipeRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * StepRecipeApplicationService 站点 Recipe 绑定应用服务
 * <p>
 * 同一站点、Recipe、产品只能有一条。限定产品有记录时不用通用绑定；没有再退回 productId=0。
 * 同一站点、同一产品只保留一个默认 Recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepRecipeApplicationService {

    private static final long GENERIC_PRODUCT = 0L;

    private final StepRecipeRepository repository;
    private final StepRepository stepRepository;
    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;

    /**
     * 解析站点可用 Recipe。限定产品优先，否则通用。
     *
     * @param stepId 站点
     * @param productId 产品，空或 0 只查通用
     * @return 命中的绑定。步骤级覆盖还没接进来
     */
    /**
     * 列出某个站点已保存的全部绑定。
     *
     * @param stepId 站点
     * @return 绑定列表
     * @throws BusinessException 站点不存在
     */
    public List<StepRecipe> list(Long stepId) {
        requireStep(stepId);
        return repository.listByStep(stepId);
    }

    public List<StepRecipe> listAllowed(Long stepId, Long productId) {
        requireStep(stepId);
        long product = productId == null ? GENERIC_PRODUCT : productId;
        if (product != GENERIC_PRODUCT) {
            List<StepRecipe> specific = repository.listByStepAndProduct(stepId, product);
            if (!specific.isEmpty()) {
                return specific;
            }
        }
        return repository.listByStepAndProduct(stepId, GENERIC_PRODUCT);
    }

    /**
     * 新增绑定。
     *
     * @param request 绑定内容
     * @throws BusinessException 主数据不存在或组合已存在
     */
    @Transactional
    public void create(StepRecipeSaveRequest request) {
        long product = productOf(request);
        requireRefs(request, product);
        if (repository.findExact(request.getStepId(), request.getRecipeId(), product).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_RECIPE_EXISTS, "站点 Recipe 绑定已存在");
        }
        StepRecipe binding = new StepRecipe();
        copy(request, binding, product);
        clearOtherDefault(binding);
        repository.save(binding);
    }

    /**
     * 修改绑定。
     *
     * @param id 主键
     * @param request 绑定内容
     * @throws BusinessException 绑定不存在或组合与其他记录冲突
     */
    @Transactional
    public void update(Long id, StepRecipeSaveRequest request) {
        StepRecipe binding = require(id);
        long product = productOf(request);
        requireRefs(request, product);
        repository.findExact(request.getStepId(), request.getRecipeId(), product)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.STEP_RECIPE_EXISTS, "站点 Recipe 绑定已存在");
                });
        copy(request, binding, product);
        clearOtherDefault(binding);
        repository.update(binding);
    }

    /**
     * 删除绑定。
     *
     * @param id 主键
     * @throws BusinessException 绑定不存在
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        repository.deleteById(id);
    }

    private void clearOtherDefault(StepRecipe binding) {
        if (binding.getIsDefault() == null || binding.getIsDefault() != 1) {
            return;
        }
        for (StepRecipe other : repository.listByStepAndProduct(binding.getStepId(), binding.getProductId())) {
            if (other.getId() != null && other.getId().equals(binding.getId())) {
                continue;
            }
            if (other.getIsDefault() != null && other.getIsDefault() == 1) {
                other.setIsDefault(0);
                repository.update(other);
            }
        }
    }

    private void copy(StepRecipeSaveRequest request, StepRecipe binding, long product) {
        binding.setStepId(request.getStepId());
        binding.setRecipeId(request.getRecipeId());
        binding.setProductId(product);
        binding.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
    }

    private void requireRefs(StepRecipeSaveRequest request, long product) {
        requireStep(request.getStepId());
        if (recipeRepository.findById(request.getRecipeId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.RECIPE_NOT_FOUND, "Recipe 不存在");
        }
        if (product != GENERIC_PRODUCT && productRepository.findById(product).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在");
        }
    }

    private void requireStep(Long stepId) {
        if (stepRepository.findById(stepId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在");
        }
    }

    private StepRecipe require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.STEP_RECIPE_NOT_FOUND, "站点 Recipe 绑定不存在"));
    }

    private static long productOf(StepRecipeSaveRequest request) {
        return request.getProductId() == null ? GENERIC_PRODUCT : request.getProductId();
    }
}
