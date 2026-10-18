package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.RouteStepRecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStepRecipe;
import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.mes.base.domain.service.RouteStepRecipeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RouteStepRecipeControllerV1 步骤 Recipe 覆盖接口
 * <p>
 * 提供覆盖的解析、新增、修改和删除。解析结果在有步骤级记录时不再包含站点级。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/route-step-recipes")
@RequiredArgsConstructor
public class RouteStepRecipeControllerV1 {

    private final RouteStepRecipeApplicationService service;

    /**
     * 解析步骤可用 Recipe。
     *
     * @param routeStepId Route 步骤
     * @param productId 产品，可空
     * @return 步骤级或站点级绑定
     */
    /**
     * 列出步骤上已保存的覆盖。
     *
     * @param routeStepId Route 步骤
     * @return 覆盖列表，不含站点级回退
     */
    @GetMapping("/owned")
    public Result<List<RouteStepRecipe>> listOwned(@RequestParam Long routeStepId) {
        return Result.success(service.listOwned(routeStepId));
    }

    @GetMapping
    public Result<List<StepRecipe>> resolve(@RequestParam Long routeStepId,
            @RequestParam(required = false) Long productId) {
        return Result.success(service.resolve(routeStepId, productId));
    }

    /**
     * 新增覆盖。
     *
     * @param request 覆盖内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增步骤 Recipe 覆盖")
    public Result<Boolean> create(@Valid @RequestBody RouteStepRecipeSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改覆盖。
     *
     * @param id 主键
     * @param request 覆盖内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改步骤 Recipe 覆盖")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody RouteStepRecipeSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除覆盖。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除步骤 Recipe 覆盖")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
