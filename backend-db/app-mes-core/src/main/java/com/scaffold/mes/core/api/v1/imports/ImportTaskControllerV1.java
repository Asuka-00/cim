package com.scaffold.mes.core.api.v1.imports;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.dto.imports.ImportErrorResponse;
import com.scaffold.mes.core.domain.dto.imports.ImportFailureRequest;
import com.scaffold.mes.core.domain.dto.imports.ImportTaskCreateRequest;
import com.scaffold.mes.core.domain.dto.imports.ImportTaskResponse;
import com.scaffold.mes.core.domain.service.imports.ImportTaskApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ImportTaskControllerV1 导入任务
 * <p>
 * 新建草稿，提交，开始，记成功行和失败行，结束任务，按任务号查询，并按状态列出。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/import-tasks")
@RequiredArgsConstructor
public class ImportTaskControllerV1 {

    private final ImportTaskApplicationService service;

    /**
     * 新建草稿。
     *
     * @param request 任务号和导入类型
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建导入任务")
    public Result<Boolean> create(@Valid @RequestBody ImportTaskCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 按任务号查询。
     *
     * @param taskNo 任务号
     * @return 状态和行数
     */
    @GetMapping
    public Result<ImportTaskResponse> show(@RequestParam String taskNo) {
        return Result.success(service.show(taskNo));
    }

    /**
     * 列出正在运行的任务。
     *
     * @return 按任务号排列
     */
    @GetMapping("/running")
    public Result<List<ImportTaskResponse>> running() {
        return Result.success(service.listRunning());
    }

    /**
     * 按状态列出任务。
     *
     * @param status 状态
     * @return 按任务号排列
     */
    @GetMapping("/by-status")
    public Result<List<ImportTaskResponse>> byStatus(@RequestParam String status,
            @RequestParam(required = false) String importType) {
        return Result.success(service.listByStatus(status, importType));
    }

    /**
     * 按导入类型列出任务。
     *
     * @param importType 导入类型
     * @return 按任务号排列
     */
    @GetMapping("/by-type")
    public Result<List<ImportTaskResponse>> byType(@RequestParam String importType) {
        return Result.success(service.listByType(importType));
    }

    /**
     * 按操作人列出任务。
     *
     * @param operator 操作人
     * @return 按任务号排列
     */
    @GetMapping("/by-operator")
    public Result<List<ImportTaskResponse>> byOperator(@RequestParam String operator,
            @RequestParam(required = false) String status) {
        return Result.success(service.listByOperator(operator, status));
    }

    /**
     * 提交草稿。
     *
     * @param id 任务
     * @return 固定为 true
     */
    @PostMapping("/{id}/submit")
    @AuditLog(operation = "提交导入任务")
    public Result<Boolean> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 作废草稿。
     *
     * @param id 任务
     * @return 固定为 true
     */
    @PostMapping("/{id}/cancel")
    @AuditLog(operation = "作废导入任务")
    public Result<Boolean> cancel(@PathVariable Long id) {
        service.cancel(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 撤回待处理的任务。
     *
     * @param id 任务
     * @return 固定为 true
     */
    @PostMapping("/{id}/withdraw")
    @AuditLog(operation = "撤回导入任务")
    public Result<Boolean> withdraw(@PathVariable Long id) {
        service.withdraw(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 开始待处理的任务。
     *
     * @param id 任务
     * @return 固定为 true
     */
    @PostMapping("/{id}/start")
    @AuditLog(operation = "开始导入任务")
    public Result<Boolean> start(@PathVariable Long id) {
        service.start(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 记一条失败行。
     *
     * @param id 任务
     * @param request 行号和错误信息
     * @return 固定为 true
     */
    @PostMapping("/{id}/failures")
    @AuditLog(operation = "记录导入失败行")
    public Result<Boolean> fail(@PathVariable Long id, @Valid @RequestBody ImportFailureRequest request) {
        service.fail(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 记一条成功行。
     *
     * @param id 任务
     * @return 固定为 true
     */
    @PostMapping("/{id}/successes")
    @AuditLog(operation = "记录导入成功行")
    public Result<Boolean> succeed(@PathVariable Long id) {
        service.succeed(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 结束运行中的任务。
     *
     * @param id 任务
     * @return 固定为 true
     */
    @PostMapping("/{id}/finish")
    @AuditLog(operation = "结束导入任务")
    public Result<Boolean> finish(@PathVariable Long id) {
        service.finish(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出失败行。
     *
     * @param id 任务
     * @return 按行号排列的失败行
     */
    @GetMapping("/{id}/failures")
    public Result<List<ImportErrorResponse>> failures(@PathVariable Long id) {
        return Result.success(service.listFailures(id));
    }

    /**
     * 按主键查询一条失败行。
     *
     * @param errorId 失败行主键
     * @return 这一条错误
     */
    @GetMapping("/errors/{errorId}")
    public Result<ImportErrorResponse> failure(@PathVariable Long errorId) {
        return Result.success(service.showFailure(errorId));
    }
}
