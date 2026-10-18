package com.scaffold.system.api.v1.log;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.audit.OperationLogQuery;
import com.scaffold.share.audit.OperationLogService;
import com.scaffold.share.audit.OperationLogVO;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * OperationLogControllerV1 操作日志查询接口
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/api/v1/audit/operation-logs")
@RequiredArgsConstructor
@Tag(name = "OperationLogController", description = "操作日志查询接口")
public class OperationLogControllerV1 {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @AuditLog(operation = "查询操作日志", logResponse = false, writeDb = false)
    @Operation(summary = "查询操作日志", description = "分页查询操作日志")
    @GetMapping
    public Result<PageDTO<OperationLogVO>> queryOperationLogList(
            @RequestParam(name = "page") int page,
            @RequestParam(name = "pageSize") int pageSize,
            OperationLogQuery query) {
        return Result.success(operationLogService.queryOperationLogPage(page, pageSize, query));
    }
}
