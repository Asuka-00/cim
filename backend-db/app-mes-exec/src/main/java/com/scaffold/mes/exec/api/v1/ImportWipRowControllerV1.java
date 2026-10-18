package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.ImportWipRowRequest;
import com.scaffold.mes.exec.domain.service.ImportWipFileApplicationService;
import com.scaffold.mes.exec.domain.service.ImportWipRowApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ImportWipRowControllerV1 按导入任务写在制
 * <p>
 * 可以一次提交多行，每一行单独记成功或失败。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/import-tasks")
@RequiredArgsConstructor
public class ImportWipRowControllerV1 {

    private final ImportWipRowApplicationService service;

    private final ImportWipFileApplicationService files;

    /**
     * 写入一行在制并记成功。
     *
     * @param id 任务
     * @param request 工单和这一行
     * @return 固定为 true
     */
    @PostMapping("/{id}/wip-rows")
    @AuditLog(operation = "导入一行在制")
    public Result<Boolean> importRow(@PathVariable Long id, @Valid @RequestBody ImportWipRowRequest request) {
        service.importRow(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 一次写入多行。
     *
     * @param id 任务
     * @param rows 各行
     * @return 固定为 true
     */
    @PostMapping("/{id}/wip-rows/batch")
    @AuditLog(operation = "导入多行在制")
    public Result<Boolean> importRows(@PathVariable Long id,
            @Valid @RequestBody List<ImportWipRowRequest> rows) {
        service.importRows(id, rows);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 按任务上的 CSV 文件导入。
     *
     * @param id 任务
     * @return 固定为 true
     */
    @PostMapping("/{id}/wip-file")
    @AuditLog(operation = "按文件导入在制")
    public Result<Boolean> importStored(@PathVariable Long id) {
        files.importStored(id);
        return Result.success(Boolean.TRUE);
    }
}
