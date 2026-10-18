package com.scaffold.mes.exec.api.v1;

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
import com.scaffold.mes.exec.domain.dto.HoldReasonSaveRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.service.HoldReasonApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HoldReasonControllerV1 Hold 原因接口
 * <p>
 * 提供原因码的查询、新增、修改和删除。被 Lot Hold 引用时不能删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/hold-reasons")
@RequiredArgsConstructor
public class HoldReasonControllerV1 {

    private final HoldReasonApplicationService service;

    /**
     * 分页查询原因码。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param reasonCode 编码模糊条件，可空
     * @return 分页结果
     */
    @GetMapping
    public Result<PageDTO<HoldReason>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String reasonCode) {
        return Result.success(service.page(page, pageSize, reasonCode));
    }

    /**
     * 新增原因码。
     *
     * @param request 原因内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新增 Hold 原因")
    public Result<Boolean> create(@Valid @RequestBody HoldReasonSaveRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修改原因码。
     *
     * @param id 主键
     * @param request 原因内容
     * @return 固定为 true
     */
    @PutMapping("/{id}")
    @AuditLog(operation = "修改 Hold 原因")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody HoldReasonSaveRequest request) {
        service.update(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 删除原因码。
     *
     * @param id 主键
     * @return 固定为 true
     */
    @DeleteMapping("/{id}")
    @AuditLog(operation = "删除 Hold 原因")
    public Result<Boolean> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success(Boolean.TRUE);
    }
}
