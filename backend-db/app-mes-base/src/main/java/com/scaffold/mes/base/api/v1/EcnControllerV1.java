package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.EcnApproveRequest;
import com.scaffold.mes.base.domain.dto.EcnCreateRequest;
import com.scaffold.mes.base.domain.entity.Ecn;
import com.scaffold.mes.base.domain.service.EcnApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * EcnControllerV1 变更申请
 * <p>
 * 新建一条草稿。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/ecns")
@RequiredArgsConstructor
public class EcnControllerV1 {

    private final EcnApplicationService service;

    /**
     * 新建草稿。
     *
     * @param request 编号、标题、类型和原因
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建变更申请")
    public Result<Boolean> create(@Valid @RequestBody EcnCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 提交评审。
     *
     * @param id 申请
     * @return 固定为 true
     */
    @PostMapping("/{id}/submit")
    @AuditLog(operation = "提交变更申请")
    public Result<Boolean> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 批准。
     *
     * @param id 申请
     * @param request 审批人
     * @return 固定为 true
     */
    @PostMapping("/{id}/approve")
    @AuditLog(operation = "批准变更申请")
    public Result<Boolean> approve(@PathVariable Long id, @Valid @RequestBody EcnApproveRequest request) {
        service.approve(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 驳回。
     *
     * @param id 申请
     * @param request 审批人
     * @return 固定为 true
     */
    @PostMapping("/{id}/reject")
    @AuditLog(operation = "驳回变更申请")
    public Result<Boolean> reject(@PathVariable Long id, @Valid @RequestBody EcnApproveRequest request) {
        service.reject(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 生效。
     *
     * @param id 申请
     * @return 固定为 true
     */
    @PostMapping("/{id}/effect")
    @AuditLog(operation = "变更申请生效")
    public Result<Boolean> effect(@PathVariable Long id) {
        service.effect(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 取消草稿。
     *
     * @param id 申请
     * @return 固定为 true
     */
    @PostMapping("/{id}/cancel")
    @AuditLog(operation = "取消变更申请")
    public Result<Boolean> cancel(@PathVariable Long id) {
        service.cancel(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部申请。
     *
     * @return 按编号排列
     */
    @GetMapping
    public Result<List<Ecn>> list() {
        return Result.success(service.list());
    }
}
