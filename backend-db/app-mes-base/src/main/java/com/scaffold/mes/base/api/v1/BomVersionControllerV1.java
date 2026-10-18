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
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.entity.BomVersion;
import com.scaffold.mes.base.domain.service.BomVersionApplicationService;
import com.scaffold.share.context.UserContextHolder;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * BomVersionControllerV1 BOM 版本接口
 * <p>
 * 提供草稿、复制、提交、通过和驳回。复制会带上全部 BOM 行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/boms/{bomId}/versions")
@RequiredArgsConstructor
public class BomVersionControllerV1 {

    private final BomVersionApplicationService service;

    /**
     * 分页查询版本。
     *
     * @param bomId BOM
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<BomVersion>> page(@PathVariable Long bomId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(service.page(bomId, page, pageSize));
    }

    /**
     * 新增草稿。
     *
     * @param bomId BOM
     * @param request 备注
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 BOM 版本")
    public Result<Boolean> create(@PathVariable Long bomId, @RequestBody RouteVersionSaveRequest request) {
        service.create(bomId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改草稿备注。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param request 备注
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 BOM 版本")
    public Result<Boolean> update(@PathVariable Long bomId, @PathVariable Long id,
            @RequestBody RouteVersionSaveRequest request) {
        service.update(bomId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 复制成新草稿，并带上全部 BOM 行。
     *
     * @param bomId BOM
     * @param id 来源版本
     * @return 固定为 true
     */
    @PostMapping("/{id}/copy")
    @AuditLog(operation = "复制 BOM 版本")
    public Result<Boolean> copy(@PathVariable Long bomId, @PathVariable Long id) {
        service.copy(bomId, id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 提交审批。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/submit")
    @AuditLog(operation = "提交 BOM 版本")
    public Result<Boolean> submit(@PathVariable Long bomId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionSubmitRequest request) {
        service.submit(bomId, id, UserContextHolder.getUserName(), request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 审批通过并生效。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/approve")
    @AuditLog(operation = "生效 BOM 版本")
    public Result<Boolean> approve(@PathVariable Long bomId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionDecisionRequest request) {
        service.approve(bomId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 驳回为草稿。
     *
     * @param bomId BOM
     * @param id 版本主键
     * @param request 审批人和意见
     * @return 固定为 true
     */
    @PostMapping("/{id}/reject")
    @AuditLog(operation = "驳回 BOM 版本")
    public Result<Boolean> reject(@PathVariable Long bomId, @PathVariable Long id,
            @Valid @RequestBody RouteVersionDecisionRequest request) {
        service.reject(bomId, id, request);
        return Result.success(Boolean.TRUE);
    }
}
