package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.BatchCreateRequest;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.service.BatchApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * BatchControllerV1 组批
 * <p>
 * 新建批次，并在进站前加入或移出 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/batches")
@RequiredArgsConstructor
public class BatchControllerV1 {

    private final BatchApplicationService service;

    /**
     * 列出批次。
     *
     * @return 按批次号排列
     */
    @GetMapping
    public Result<List<Batch>> list() {
        return Result.success(service.list());
    }

    /**
     * 新建批次。
     *
     * @param request 批次号、站点和容量
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建批次")
    public Result<Boolean> create(@Valid @RequestBody BatchCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 加入 Lot。
     *
     * @param batchId 批次
     * @param lotId Lot
     * @return 固定为 true
     */
    @PostMapping("/{batchId}/lots/{lotId}")
    @AuditLog(operation = "加入批次")
    public Result<Boolean> add(@PathVariable Long batchId, @PathVariable Long lotId) {
        service.add(batchId, lotId);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出仍在批内的成员。
     *
     * @param batchId 批次
     * @return 按序号排列
     */
    @GetMapping("/{batchId}/lots")
    public Result<List<BatchLot>> members(@PathVariable Long batchId) {
        return Result.success(service.members(batchId));
    }

    /**
     * 移出 Lot。
     *
     * @param batchId 批次
     * @param lotId Lot
     * @return 固定为 true
     */
    @PostMapping("/{batchId}/lots/{lotId}/remove")
    @AuditLog(operation = "移出批次")
    public Result<Boolean> remove(@PathVariable Long batchId, @PathVariable Long lotId) {
        service.remove(batchId, lotId);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 取消尚未进站的批次。
     *
     * @param batchId 批次
     * @return 固定为 true
     */
    @PostMapping("/{batchId}/cancel")
    @AuditLog(operation = "取消批次")
    public Result<Boolean> cancel(@PathVariable Long batchId) {
        service.cancel(batchId);
        return Result.success(Boolean.TRUE);
    }
}
