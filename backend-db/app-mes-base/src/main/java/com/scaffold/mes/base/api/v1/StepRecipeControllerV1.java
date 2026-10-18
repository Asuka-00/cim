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

import com.scaffold.mes.base.domain.dto.StepRecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.StepRecipe;
import com.scaffold.mes.base.domain.service.StepRecipeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * StepRecipeControllerV1 站点 Recipe 绑定接口
 * <p>
 * 提供绑定的查询、解析、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/step-recipes")
@RequiredArgsConstructor
public class StepRecipeControllerV1 {

    private final StepRecipeApplicationService service;

    /**
     * 解析站点可用 Recipe。限定产品优先，否则通用。
     *
     * @param stepId 站点
     * @param productId 产品，可空
     * @return 命中的绑定
     */
    /**
     * 列出站点上已保存的全部绑定。
     *
     * @param stepId 站点
     * @return 绑定列表
     */
    @GetMapping("/owned")
    public Result<List<StepRecipe>> list(@RequestParam Long stepId) {
        return Result.success(service.list(stepId));
    }

    @GetMapping
    public Result<List<StepRecipe>> listAllowed(@RequestParam Long stepId,
            @RequestParam(required = false) Long productId) {
        return Result.success(service.listAllowed(stepId, productId));
    }

    /**
     * 新增绑定。
     *
     * @param request 绑定内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增站点 Recipe 绑定")
    public Result<Boolean> create(@Valid @RequestBody StepRecipeSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改绑定。
     *
     * @param id 主键
     * @param request 绑定内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改站点 Recipe 绑定")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody StepRecipeSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除绑定。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除站点 Recipe 绑定")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
