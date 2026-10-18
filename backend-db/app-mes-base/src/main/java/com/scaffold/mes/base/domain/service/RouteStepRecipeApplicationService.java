package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteStepRecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteStepRecipe;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRecipeRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepRecipeApplicationService 步骤 Recipe 覆盖应用服务
 * <p>
 * 查找顺序：步骤加产品、步骤通用、再退回站点级。步骤级只要命中一层，就整组替换站点级，不合并。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RouteStepRecipeApplicationService {

    private static final long GENERIC_PRODUCT = 0L;

    private final RouteStepRecipeRepository repository;
    private final RouteStepRepository routeStepRepository;
    private final RouteVersionRepository versionRepository;
    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final StepRecipeApplicationService stepRecipeService;

    /**
     * 解析这个步骤可用的 Recipe。
     *
     * @param routeStepId Route 步骤
     * @param productId 产品，空或 0 只看通用
     * @return 步骤级命中则只返回步骤级，否则返回站点级
     * @throws BusinessException 步骤不存在
     */
    /**
     * 列出某个步骤上已保存的覆盖，不退回站点级。
     *
     * @param routeStepId Route 步骤
     * @return 该步骤的全部覆盖
     * @throws BusinessException 步骤不存在
     */
    public List<RouteStepRecipe> listOwned(Long routeStepId) {
        RouteStep routeStep = requireStep(routeStepId);
        return repository.listByVersion(routeStep.getRouteVersionId()).stream()
                .filter(row -> routeStepId.equals(row.getRouteStepId()))
                .toList();
    }

    public List<StepRecipe> resolve(Long routeStepId, Long productId) {
        RouteStep routeStep = requireStep(routeStepId);
        long product = productId == null ? GENERIC_PRODUCT : productId;
        if (product != GENERIC_PRODUCT) {
            List<RouteStepRecipe> specific = repository.listByStepAndProduct(routeStepId, product);
            if (!specific.isEmpty()) {
                return specific.stream().map(this::asStepRecipe).toList();
            }
        }
        List<RouteStepRecipe> generic = repository.listByStepAndProduct(routeStepId, GENERIC_PRODUCT);
        if (!generic.isEmpty()) {
            return generic.stream().map(this::asStepRecipe).toList();
        }
        return stepRecipeService.listAllowed(routeStep.getStepId(), productId);
    }

    /**
     * 在草稿版本上新增覆盖。
     *
     * @param request 覆盖内容
     * @throws BusinessException 版本不是草稿，主数据不存在，或组合已存在
     */
    @Transactional
    public void create(RouteStepRecipeSaveRequest request) {
        RouteStep routeStep = requireDraftStep(request.getRouteStepId());
        long product = productOf(request);
        requireRefs(request, product);
        if (repository.findExact(request.getRouteStepId(), request.getRecipeId(), product).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_RECIPE_EXISTS, "步骤 Recipe 覆盖已存在");
        }
        RouteStepRecipe binding = new RouteStepRecipe();
        binding.setRouteVersionId(routeStep.getRouteVersionId());
        copy(request, binding, product);
        clearOtherDefault(binding);
        repository.save(binding);
    }

    /**
     * 修改草稿版本上的覆盖。
     *
     * @param id 主键
     * @param request 覆盖内容
     * @throws BusinessException 覆盖不存在，版本不是草稿，或组合冲突
     */
    @Transactional
    public void update(Long id, RouteStepRecipeSaveRequest request) {
        RouteStepRecipe binding = require(id);
        requireDraftStep(binding.getRouteStepId());
        RouteStep routeStep = requireDraftStep(request.getRouteStepId());
        long product = productOf(request);
        requireRefs(request, product);
        repository.findExact(request.getRouteStepId(), request.getRecipeId(), product)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_RECIPE_EXISTS, "步骤 Recipe 覆盖已存在");
                });
        binding.setRouteVersionId(routeStep.getRouteVersionId());
        copy(request, binding, product);
        clearOtherDefault(binding);
        repository.update(binding);
    }

    /**
     * 删除草稿版本上的覆盖。
     *
     * @param id 主键
     * @throws BusinessException 覆盖不存在或版本不是草稿
     */
    @Transactional
    public void delete(Long id) {
        RouteStepRecipe binding = require(id);
        requireDraftStep(binding.getRouteStepId());
        repository.deleteById(id);
    }

    private StepRecipe asStepRecipe(RouteStepRecipe source) {
        StepRecipe view = new StepRecipe();
        view.setId(source.getId());
        view.setRecipeId(source.getRecipeId());
        view.setProductId(source.getProductId());
        view.setIsDefault(source.getIsDefault());
        return view;
    }

    private void clearOtherDefault(RouteStepRecipe binding) {
        if (binding.getIsDefault() == null || binding.getIsDefault() != 1) {
            return;
        }
        for (RouteStepRecipe other : repository.listByStepAndProduct(binding.getRouteStepId(), binding.getProductId())) {
            if (other.getId() != null && other.getId().equals(binding.getId())) {
                continue;
            }
            if (other.getIsDefault() != null && other.getIsDefault() == 1) {
                other.setIsDefault(0);
                repository.update(other);
            }
        }
    }

    private void copy(RouteStepRecipeSaveRequest request, RouteStepRecipe binding, long product) {
        binding.setRouteStepId(request.getRouteStepId());
        binding.setRecipeId(request.getRecipeId());
        binding.setProductId(product);
        binding.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
    }

    private void requireRefs(RouteStepRecipeSaveRequest request, long product) {
        if (recipeRepository.findById(request.getRecipeId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.RECIPE_NOT_FOUND, "Recipe 不存在");
        }
        if (product != GENERIC_PRODUCT && productRepository.findById(product).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在");
        }
    }

    private RouteStep requireDraftStep(Long routeStepId) {
        RouteStep step = requireStep(routeStepId);
        RouteVersion version = versionRepository.findById(step.getRouteVersionId())
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_FOUND, "Route 版本不存在"));
        if (!"DRAFT".equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_DRAFT, "只有草稿版本可以修改步骤 Recipe");
        }
        return step;
    }

    private RouteStep requireStep(Long routeStepId) {
        return routeStepRepository.findById(routeStepId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_STEP_NOT_FOUND, "Route 步骤不存在"));
    }

    private RouteStepRecipe require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_STEP_RECIPE_NOT_FOUND, "步骤 Recipe 覆盖不存在"));
    }

    private static long productOf(RouteStepRecipeSaveRequest request) {
        return request.getProductId() == null ? GENERIC_PRODUCT : request.getProductId();
    }
}
