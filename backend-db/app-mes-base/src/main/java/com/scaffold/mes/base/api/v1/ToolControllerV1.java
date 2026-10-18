package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.ToolCheckoutRequest;
import com.scaffold.mes.base.domain.dto.ToolCreateRequest;
import com.scaffold.mes.base.domain.entity.Tool;
import com.scaffold.mes.base.domain.service.ToolApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ToolControllerV1 工具领用
 * <p>
 * 可用工具装到设备后变为 IN_USE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/tools")
@RequiredArgsConstructor
public class ToolControllerV1 {

    private final ToolApplicationService service;

    /**
     * 新建工具。
     *
     * @param request 编码、名称和类型
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建工具")
    public Result<Boolean> create(@Valid @RequestBody ToolCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 领用到设备。
     *
     * @param toolId 工具
     * @param request 设备
     * @return 固定为 true
     */
    @PostMapping("/{toolId}/checkout")
    @AuditLog(operation = "工具领用")
    public Result<Boolean> checkout(@PathVariable Long toolId, @Valid @RequestBody ToolCheckoutRequest request) {
        service.checkout(toolId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 归还工具。
     *
     * @param toolId 工具
     * @return 固定为 true
     */
    @PostMapping("/{toolId}/return")
    @AuditLog(operation = "工具归还")
    public Result<Boolean> returnTool(@PathVariable Long toolId) {
        service.returnTool(toolId);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部工具。
     *
     * @return 按编码排列
     */
    @GetMapping
    public Result<List<Tool>> list() {
        return Result.success(service.list());
    }
}
