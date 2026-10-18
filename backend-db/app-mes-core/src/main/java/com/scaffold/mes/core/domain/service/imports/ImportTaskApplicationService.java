package com.scaffold.mes.core.domain.service.imports;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.imports.ImportErrorResponse;
import com.scaffold.mes.core.domain.dto.imports.ImportFailureRequest;
import com.scaffold.mes.core.domain.dto.imports.ImportTaskCreateRequest;
import com.scaffold.mes.core.domain.dto.imports.ImportTaskResponse;
import com.scaffold.mes.core.domain.entity.imports.ImportError;
import com.scaffold.mes.core.domain.entity.imports.ImportTask;
import com.scaffold.mes.core.domain.repository.imports.ImportErrorRepository;
import com.scaffold.mes.core.domain.repository.imports.ImportTaskRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ImportTaskApplicationService 导入任务
 * <p>
 * 新建时状态固定为 DRAFT。再次新建草稿时清掉原来的失败行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ImportTaskApplicationService {

    private static final Set<String> TYPES = Set.of(
            "PRODUCT", "ROUTE", "STEP", "EQUIPMENT", "MATERIAL", "BOM", "RECIPE", "MATERIAL_LOT", "WIP_INIT");

    private static final Set<String> MODES = Set.of("VALIDATE_ONLY", "COMMIT");

    private static final Set<String> ON_ERRORS = Set.of("SKIP", "ROLLBACK");

    private static final Set<String> STATUSES = Set.of(
            "DRAFT", "PENDING", "RUNNING", "SUCCESS", "PARTIAL", "FAILED", "CANCELLED");

    private final ImportTaskRepository repository;

    private final ImportErrorRepository errors;

    private final Clock clock;

    /**
     * 新建草稿。
     *
     * @param request 任务号和导入类型
     * @throws BusinessException 任务号已被未作废的任务占用，类型、模式或出错策略不合法
     */
    @Transactional
    public void create(ImportTaskCreateRequest request) {
        if (!TYPES.contains(request.getImportType())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入类型不合法");
        }
        String mode = request.getMode() == null || request.getMode().isBlank() ? "COMMIT" : request.getMode();
        if (!MODES.contains(mode)) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入模式不合法");
        }
        String onError = request.getOnError() == null || request.getOnError().isBlank() ? "SKIP" : request.getOnError();
        if (!ON_ERRORS.contains(onError)) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "出错策略不合法");
        }
        if (request.getFileRef() != null && request.getFileRef().length() > 256) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "文件引用不能超过 256 字");
        }
        if (request.getOperator() != null && request.getOperator().length() > 64) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "操作人不能超过 64 字");
        }
        if (request.getTaskNo() == null || request.getTaskNo().isBlank()) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "任务号不能为空");
        }
        if (request.getTaskNo().length() > 64) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "任务号不能超过 64 字");
        }
        ImportTask existing = repository.findByNo(request.getTaskNo()).orElse(null);
        if (existing != null && !"CANCELLED".equals(existing.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "任务号已存在");
        }
        ImportTask task = existing == null ? new ImportTask() : existing;
        task.setTaskNo(request.getTaskNo());
        task.setImportType(request.getImportType());
        task.setMode(mode);
        task.setOnError(onError);
        task.setFileRef(request.getFileRef() == null || request.getFileRef().isBlank() ? null : request.getFileRef());
        task.setOperator(request.getOperator() == null || request.getOperator().isBlank() ? null : request.getOperator());
        task.setStatus("DRAFT");
        task.setFailRows(0);
        task.setSuccessRows(0);
        task.setTotalRows(0);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        if (existing == null) {
            repository.save(task);
        } else {
            if (existing.getId() != null) {
                errors.deleteByTask(existing.getId());
            }
            repository.update(task);
        }
    }

    /**
     * 提交草稿。
     *
     * @param id 任务
     * @throws BusinessException 任务不存在，或不是草稿
     */
    @Transactional
    public void submit(Long id) {
        ImportTask task = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"DRAFT".equals(task.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有草稿可以提交");
        }
        task.setStatus("PENDING");
        repository.update(task);
    }

    /**
     * 作废草稿。
     *
     * @param id 任务
     * @throws BusinessException 任务不存在，或不是草稿
     */
    @Transactional
    public void cancel(Long id) {
        ImportTask task = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"DRAFT".equals(task.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有草稿可以作废");
        }
        task.setStatus("CANCELLED");
        repository.update(task);
    }

    /**
     * 撤回待处理的任务。
     *
     * @param id 任务
     * @throws BusinessException 任务不存在，或不是待处理
     */
    @Transactional
    public void withdraw(Long id) {
        ImportTask task = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有待处理的任务可以撤回");
        }
        task.setStatus("DRAFT");
        repository.update(task);
    }

    /**
     * 开始待处理的任务。
     *
     * @param id 任务
     * @throws BusinessException 任务不存在，或不是待处理
     */
    @Transactional
    public void start(Long id) {
        ImportTask task = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有待处理的任务可以开始");
        }
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now(clock));
        repository.update(task);
    }

    /**
     * 给运行中的任务记一条失败行。
     *
     * @param id 任务
     * @param request 行号和错误信息
     * @throws BusinessException 任务不在运行中，行号小于 1，错误信息为空，或该行号已记过
     */
    @Transactional
    public void fail(Long id, ImportFailureRequest request) {
        if (request.getRowNo() == null || request.getRowNo() < 1
                || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "失败行号和错误信息不能为空");
        }
        if (request.getMessage().length() > 512) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "错误信息不能超过 512 字");
        }
        if (request.getRawData() != null && request.getRawData().length() > 1024) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "原始内容不能超过 1024 字");
        }
        if (request.getErrorCode() != null && request.getErrorCode().length() > 32) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "错误码不能超过 32 字");
        }
        ImportTask task = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"RUNNING".equals(task.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有运行中的任务可以记失败行");
        }
        if (errors.exists(id, request.getRowNo())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "该行号已经记过失败");
        }
        int failed = task.getFailRows() == null ? 0 : task.getFailRows();
        task.setFailRows(failed + 1);
        addTotal(task);
        repository.update(task);
        ImportError error = new ImportError();
        error.setTaskId(id);
        error.setRowNo(request.getRowNo());
        error.setMessage(request.getMessage());
        error.setErrorCode(request.getErrorCode() == null || request.getErrorCode().isBlank()
                ? null : request.getErrorCode());
        error.setRawData(request.getRawData() == null || request.getRawData().isBlank()
                ? null : request.getRawData());
        errors.save(error);
    }

    /**
     * 给运行中的任务记一条成功行。
     *
     * @param id 任务
     * @throws BusinessException 任务不存在，不在运行中，或只做校验
     */
    @Transactional
    public void succeed(Long id) {
        ImportTask task = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"RUNNING".equals(task.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有运行中的任务可以记成功行");
        }
        if ("VALIDATE_ONLY".equals(task.getMode())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只校验的任务不能记成功行");
        }
        int succeeded = task.getSuccessRows() == null ? 0 : task.getSuccessRows();
        task.setSuccessRows(succeeded + 1);
        addTotal(task);
        repository.update(task);
    }

    private static void addTotal(ImportTask task) {
        int total = task.getTotalRows() == null ? 0 : task.getTotalRows();
        task.setTotalRows(total + 1);
    }

    /**
     * 结束运行中的任务。
     *
     * @param id 任务
     * @throws BusinessException 任务不在运行中，或成功行数加失败行数不等于总行数
     */
    @Transactional
    public void finish(Long id) {
        ImportTask task = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"RUNNING".equals(task.getStatus())) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有运行中的任务可以结束");
        }
        int failed = task.getFailRows() == null ? 0 : task.getFailRows();
        int success = task.getSuccessRows() == null ? 0 : task.getSuccessRows();
        int total = task.getTotalRows() == null ? 0 : task.getTotalRows();
        if (success + failed != total) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "成功行数加失败行数必须等于总行数");
        }
        boolean rollback = "ROLLBACK".equals(task.getOnError()) && failed > 0;
        task.setStatus(rollback || (failed > 0 && success == 0) ? "FAILED" : failed == 0 ? "SUCCESS" : "PARTIAL");
        task.setFinishedAt(LocalDateTime.now(clock));
        repository.update(task);
    }

    /**
     * 列出这个任务的失败行。
     *
     * @param id 任务
     * @return 按行号排列的失败行
     * @throws BusinessException 任务不存在
     */
    public List<ImportErrorResponse> listFailures(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        return errors.listByTask(id).stream()
                .sorted((left, right) -> Integer.compare(
                        left.getRowNo() == null ? 0 : left.getRowNo(),
                        right.getRowNo() == null ? 0 : right.getRowNo()))
                .map(row -> new ImportErrorResponse(row.getId(), row.getTaskId(), row.getRowNo(), row.getMessage(),
                        row.getErrorCode(), row.getRawData()))
                .toList();
    }

    /**
     * 按主键查询一条失败行。
     *
     * @param id 失败行主键
     * @return 这一条错误
     * @throws BusinessException 失败行不存在
     */
    public ImportErrorResponse showFailure(Long id) {
        ImportError row = errors.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "失败行不存在"));
        return new ImportErrorResponse(row.getId(), row.getTaskId(), row.getRowNo(), row.getMessage(),
                row.getErrorCode(), row.getRawData());
    }

    /**
     * 按任务号查询。
     *
     * @param taskNo 任务号
     * @return 状态和行数
     * @throws BusinessException 任务号为空，或任务不存在
     */
    public ImportTaskResponse show(String taskNo) {
        if (taskNo == null || taskNo.isBlank()) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "任务号不能为空");
        }
        if (taskNo.length() > 64) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "任务号不能超过 64 字");
        }
        ImportTask task = repository.findByNo(taskNo)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        return toResponse(task);
    }

    /**
     * 列出正在运行的任务。
     *
     * @return 按任务号排列
     */
    public List<ImportTaskResponse> listRunning() {
        return listByStatus("RUNNING");
    }

    /**
     * 按状态列出任务。
     *
     * @param status 状态
     * @return 按任务号排列
     * @throws BusinessException 状态不合法
     */
    public List<ImportTaskResponse> listByStatus(String status) {
        return listByStatus(status, null);
    }

    /**
     * 按状态列出任务，导入类型非空时再收窄。
     *
     * @param status 状态
     * @param importType 导入类型，可空
     * @return 按任务号排列
     * @throws BusinessException 状态或类型不合法
     */
    public List<ImportTaskResponse> listByStatus(String status, String importType) {
        if (status == null || !STATUSES.contains(status)) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入状态不合法");
        }
        if (importType == null || importType.isBlank()) {
            return repository.listByStatus(status).stream()
                    .map(ImportTaskApplicationService::toResponse)
                    .toList();
        }
        if (!TYPES.contains(importType)) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入类型不合法");
        }
        return repository.listByStatusAndType(status, importType).stream()
                .map(ImportTaskApplicationService::toResponse)
                .toList();
    }

    /**
     * 按导入类型列出任务。
     *
     * @param importType 导入类型
     * @return 按任务号排列
     * @throws BusinessException 类型不合法
     */
    public List<ImportTaskResponse> listByType(String importType) {
        if (importType == null || !TYPES.contains(importType)) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入类型不合法");
        }
        return repository.listByType(importType).stream()
                .map(ImportTaskApplicationService::toResponse)
                .toList();
    }

    /**
     * 按操作人列出任务。
     *
     * @param operator 操作人
     * @return 按任务号排列
     * @throws BusinessException 操作人为空
     */
    public List<ImportTaskResponse> listByOperator(String operator) {
        return listByOperator(operator, null);
    }

    /**
     * 按操作人列出任务，状态非空时再收窄。
     *
     * @param operator 操作人
     * @param status 状态，可空
     * @return 按任务号排列
     * @throws BusinessException 操作人为空，或状态不合法
     */
    public List<ImportTaskResponse> listByOperator(String operator, String status) {
        if (operator == null || operator.isBlank()) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "操作人不能为空");
        }
        if (status == null || status.isBlank()) {
            return repository.listByOperator(operator).stream()
                    .map(ImportTaskApplicationService::toResponse)
                    .toList();
        }
        if (!STATUSES.contains(status)) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入状态不合法");
        }
        return repository.listByOperatorAndStatus(operator, status).stream()
                .map(ImportTaskApplicationService::toResponse)
                .toList();
    }

    private static ImportTaskResponse toResponse(ImportTask task) {
        return new ImportTaskResponse(task.getId(), task.getTaskNo(), task.getImportType(), task.getMode(),
                task.getOnError(), task.getStatus(),
                count(task.getTotalRows()), count(task.getSuccessRows()), count(task.getFailRows()),
                task.getFileRef(), task.getOperator(), task.getStartedAt(), task.getFinishedAt());
    }

    private static int count(Integer value) {
        return value == null ? 0 : value;
    }
}
