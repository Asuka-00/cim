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
import com.scaffold.mes.base.domain.dto.RecipeSaveRequest;
import com.scaffold.mes.base.domain.entity.Recipe;
import com.scaffold.mes.base.domain.service.RecipeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RecipeControllerV1 Recipe 接口
 * <p>
 * 提供 Recipe 主数据的查询、新增、修改和删除。版本不在本接口。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/recipes")
@RequiredArgsConstructor
public class RecipeControllerV1 {

    private final RecipeApplicationService service;

    /**
     * 分页查询 Recipe。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param recipeCode 编码模糊条件，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<Recipe>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String recipeCode) {
        return Result.success(service.page(page, pageSize, recipeCode));
    }

    /**
     * 新增 Recipe。
     *
     * @param request Recipe 内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 Recipe")
    public Result<Boolean> create(@Valid @RequestBody RecipeSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改 Recipe。
     *
     * @param id 主键
     * @param request Recipe 内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 Recipe")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody RecipeSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除 Recipe。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除 Recipe")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
