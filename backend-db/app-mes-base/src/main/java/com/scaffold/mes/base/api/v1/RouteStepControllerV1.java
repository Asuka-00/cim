package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.RouteStepSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.service.RouteStepApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RouteStepControllerV1 Route 步骤接口
 * <p>
 * 提供某个 Route 版本下步骤的查询、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/route-versions/{routeVersionId}/steps")
@RequiredArgsConstructor
public class RouteStepControllerV1 {

    private final RouteStepApplicationService service;

    /**
     * 列出步骤。
     *
     * @param routeVersionId Route 版本
     * @return 按序号排序的步骤
     */
    @GetMapping
    public Result<List<RouteStep>> list(@PathVariable Long routeVersionId) {
        return Result.success(service.list(routeVersionId));
    }

    /**
     * 新增步骤。
     *
     * @param routeVersionId Route 版本
     * @param request 步骤内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 Route 步骤")
    public Result<Boolean> create(@PathVariable Long routeVersionId, @Valid @RequestBody RouteStepSaveRequest request) {
        service.create(routeVersionId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改步骤。
     *
     * @param routeVersionId Route 版本
     * @param id 步骤主键
     * @param request 步骤内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 Route 步骤")
    public Result<Boolean> update(@PathVariable Long routeVersionId, @PathVariable Long id,
            @Valid @RequestBody RouteStepSaveRequest request) {
        service.update(routeVersionId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除步骤。
     *
     * @param routeVersionId Route 版本
     * @param id 步骤主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除 Route 步骤")
    public Result<Boolean> delete(@PathVariable Long routeVersionId, @PathVariable Long id) {
        service.delete(routeVersionId, id);
        return Result.success(Boolean.TRUE);
    }
}
