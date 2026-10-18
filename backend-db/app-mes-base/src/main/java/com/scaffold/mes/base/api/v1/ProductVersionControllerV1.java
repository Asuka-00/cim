package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.dto.ProductVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.ProductVersion;
import com.scaffold.mes.base.domain.service.ProductVersionApplicationService;
import com.scaffold.share.context.UserContextHolder;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ProductVersionControllerV1 产品版本接口
 * <p>
 * 提供草稿、提交、通过和驳回。生效前 Route 和 BOM 都必须已有生效版本。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/products/{productId}/versions")
@RequiredArgsConstructor
public class ProductVersionControllerV1 {

    private final ProductVersionApplicationService service;

    /**
     * 分页查询版本。
     *
     * @param productId 产品
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<ProductVersion>> page(@PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.page(productId, page, pageSize));
    }

    /**
     * 新增草稿。
     *
     * @param productId 产品
     * @param request 绑定和备注
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增产品版本")
    public Result<Boolean> create(@PathVariable Long productId, @RequestBody ProductVersionSaveRequest request) {
        service.create(productId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改草稿。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param request 绑定和备注
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改产品版本")
    public Result<Boolean> update(@PathVariable Long productId, @PathVariable Long id,
            @RequestBody ProductVersionSaveRequest request) {
        service.update(productId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 复制成新草稿。
     *
     * @param productId 产品
     * @param id 来源版本
     * @return 固定为 true
     */
    @PostMapping("/{id}/copy")
    @AuditLog(operation = "复制产品版本")
    public Result<Boolean> copy(@PathVariable Long productId, @PathVariable Long id) {
        service.copy(productId, id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 提交审批。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/submit")
    @AuditLog(operation = "提交产品版本")
    public Result<Boolean> submit(@PathVariable Long productId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionSubmitRequest request) {
        service.submit(productId, id, UserContextHolder.getUserName(), request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 审批通过并生效。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/approve")
    @AuditLog(operation = "生效产品版本")
    public Result<Boolean> approve(@PathVariable Long productId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionDecisionRequest request) {
        service.approve(productId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 驳回为草稿。
     *
     * @param productId 产品
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/reject")
    @AuditLog(operation = "驳回产品版本")
    public Result<Boolean> reject(@PathVariable Long productId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionDecisionRequest request) {
        service.reject(productId, id, request);
        return Result.success(Boolean.TRUE);
    }
}
