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

import com.scaffold.mes.base.domain.dto.RouteTimeLinkSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteTimeLink;
import com.scaffold.mes.base.domain.service.RouteTimeLinkApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RouteTimeLinkControllerV1 Time Link 接口
 * <p>
 * 提供某个 Route 版本下时间链的查询、新增、修改和删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/route-versions/{routeVersionId}/time-links")
@RequiredArgsConstructor
public class RouteTimeLinkControllerV1 {

    private final RouteTimeLinkApplicationService service;

    /**
     * 列出 Time Link。
     *
     * @param routeVersionId Route 版本
     * @return 时间链列表
     */
    @GetMapping
    public Result<List<RouteTimeLink>> list(@PathVariable Long routeVersionId) {
        return Result.success(service.list(routeVersionId));
    }

    /**
     * 新增 Time Link。
     *
     * @param routeVersionId Route 版本
     * @param request 约束内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 Time Link")
    public Result<Boolean> create(@PathVariable Long routeVersionId, @Valid @RequestBody RouteTimeLinkSaveRequest request) {
        service.create(routeVersionId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改 Time Link。
     *
     * @param routeVersionId Route 版本
     * @param id 主键
     * @param request 约束内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 Time Link")
    public Result<Boolean> update(@PathVariable Long routeVersionId, @PathVariable Long id,
            @Valid @RequestBody RouteTimeLinkSaveRequest request) {
        service.update(routeVersionId, id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除 Time Link。
     *
     * @param routeVersionId Route 版本
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除 Time Link")
    public Result<Boolean> delete(@PathVariable Long routeVersionId, @PathVariable Long id) {
        service.delete(routeVersionId, id);
        return Result.success(Boolean.TRUE);
    }
}
