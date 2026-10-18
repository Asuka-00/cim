package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.dto.RecipeVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.RecipeVersion;
import com.scaffold.mes.base.domain.service.RecipeVersionApplicationService;
import com.scaffold.share.context.UserContextHolder;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RecipeVersionControllerV1 Recipe 版本接口
 * <p>
 * 提供版本的查询、新增、修改、复制、提交、通过、驳回和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/recipes/{recipeId}/versions")
@RequiredArgsConstructor
public class RecipeVersionControllerV1 {

    private final RecipeVersionApplicationService service;

    /**
     * 分页查询版本。
     *
     * @param recipeId Recipe
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<RecipeVersion>> page(@PathVariable Long recipeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.page(recipeId, page, pageSize));
    }

    /**
     * 新增草稿。
     *
     * @param recipeId Recipe
     * @param request 参数说明
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 Recipe 版本")
    public Result<Boolean> create(@PathVariable Long recipeId, @Valid @RequestBody RecipeVersionSaveRequest request) {
        service.create(recipeId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改草稿。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @param request 参数说明
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 Recipe 版本")
    public Result<Boolean> update(@PathVariable Long recipeId, @PathVariable Long id,
            @Valid @RequestBody RecipeVersionSaveRequest request) {
        service.update(recipeId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 复制出下一个草稿。
     *
     * @param recipeId Recipe
     * @param id 来源版本
     * @return 固定为 true
     */
    @PostMapping("/{id}/copy")
    @AuditLog(operation = "复制 Recipe 版本")
    public Result<Boolean> copy(@PathVariable Long recipeId, @PathVariable Long id) {
        service.copy(recipeId, id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 提交审批。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/submit")
    @AuditLog(operation = "提交 Recipe 版本")
    public Result<Boolean> submit(@PathVariable Long recipeId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionSubmitRequest request) {
        service.submit(recipeId, id, UserContextHolder.getUserName(), request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 审批通过并生效。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/approve")
    @AuditLog(operation = "生效 Recipe 版本")
    public Result<Boolean> approve(@PathVariable Long recipeId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionDecisionRequest request) {
        service.approve(recipeId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 驳回为草稿。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/reject")
    @AuditLog(operation = "驳回 Recipe 版本")
    public Result<Boolean> reject(@PathVariable Long recipeId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionDecisionRequest request) {
        service.reject(recipeId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除草稿。
     *
     * @param recipeId Recipe
     * @param id 版本主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除 Recipe 版本")
    public Result<Boolean> delete(@PathVariable Long recipeId, @PathVariable Long id) {
        service.delete(recipeId, id);
        return Result.success(Boolean.TRUE);
    }
}
