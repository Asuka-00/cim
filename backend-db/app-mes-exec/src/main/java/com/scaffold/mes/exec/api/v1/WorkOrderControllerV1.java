package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.dto.WorkOrderSaveRequest;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.service.WorkOrderApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * WorkOrderControllerV1 工单接口
 * <p>
 * 创建时固化 Route 和 BOM 版本。下达和取消只作用于已创建且未开批的工单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/work-orders")
@RequiredArgsConstructor
public class WorkOrderControllerV1 {

    private final WorkOrderApplicationService service;

    /**
     * 分页查询工单。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param woNo 工单号模糊条件，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<WorkOrder>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String woNo) {
        return Result.success(service.page(page, pageSize, woNo));
    }

    /**
     * 创建工单。
     *
     * @param request 工单号、产品版本和计划量
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "创建工单")
    public Result<Boolean> create(@Valid @RequestBody WorkOrderSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 下达工单。
     *
     * @param id 主键
     * @return 线边不够的物料提示。工单已经下达
     */
    @PostMapping("/{id}/release")
    @AuditLog(operation = "下达工单")
    public Result<List<String>> release(@PathVariable Long id) {
        return Result.success(service.release(id));
    }

    /**
     * 取消工单。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @PostMapping("/{id}/cancel")
    @AuditLog(operation = "取消工单")
    public Result<Boolean> cancel(@PathVariable Long id) {
        service.cancel(id);
        return Result.success(Boolean.TRUE);
    }
}
