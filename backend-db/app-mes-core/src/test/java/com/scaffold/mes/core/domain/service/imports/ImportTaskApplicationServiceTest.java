package com.scaffold.mes.core.domain.service.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

/**
 * 新建导入任务时状态是草稿。再次新建草稿时清掉原来的失败行。
 */
class ImportTaskApplicationServiceTest {

    private ImportTaskRepository repository;
    private ImportErrorRepository errors;
    private ImportTaskApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ImportTaskRepository.class);
        errors = mock(ImportErrorRepository.class);
        when(repository.findByNo("IMP-1")).thenReturn(Optional.empty());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new ImportTaskApplicationService(repository, errors, clock);
    }

    @Test
    void createsWipInitDraft() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "WIP_INIT".equals(row.getImportType())
                        && "DRAFT".equals(row.getStatus())
                        && "COMMIT".equals(row.getMode())
                        && "SKIP".equals(row.getOnError())));
    }

    @Test
    void createsRollback() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setOnError("ROLLBACK");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "ROLLBACK".equals(row.getOnError())));
    }

    @Test
    void badOnErrorRejected() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setOnError("ABORT");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void createsValidateOnly() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setMode("VALIDATE_ONLY");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "VALIDATE_ONLY".equals(row.getMode())));
    }

    @Test
    void badModeRejected() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setMode("DRY_RUN");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void duplicateNoRejected() {
        when(repository.findByNo("IMP-1")).thenReturn(Optional.of(new ImportTask()));
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void recreatesDraftFromCancelledNo() {
        ImportTask cancelled = new ImportTask();
        cancelled.setId(7L);
        cancelled.setTaskNo("IMP-1");
        cancelled.setStatus("CANCELLED");
        cancelled.setImportType("MATERIAL");
        cancelled.setFailRows(2);
        cancelled.setStartedAt(LocalDateTime.parse("2026-10-16T12:00"));
        when(repository.findByNo("IMP-1")).thenReturn(Optional.of(cancelled));
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");

        service.create(request);

        verify(errors).deleteByTask(7L);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "DRAFT".equals(row.getStatus())
                        && "WIP_INIT".equals(row.getImportType())
                        && Integer.valueOf(0).equals(row.getFailRows())
                        && row.getStartedAt() == null));
    }

    @Test
    void badTypeRejected() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("LOT");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void submitMovesDraftToPending() {
        ImportTask task = new ImportTask();
        task.setStatus("DRAFT");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.submit(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "PENDING".equals(row.getStatus())));
    }

    @Test
    void submitRejectsPending() {
        ImportTask task = new ImportTask();
        task.setStatus("PENDING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.submit(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void cancelMovesDraftToCancelled() {
        ImportTask task = new ImportTask();
        task.setStatus("DRAFT");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.cancel(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "CANCELLED".equals(row.getStatus())));
    }

    @Test
    void cancelRejectsPending() {
        ImportTask task = new ImportTask();
        task.setStatus("PENDING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.cancel(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void withdrawMovesPendingToDraft() {
        ImportTask task = new ImportTask();
        task.setStatus("PENDING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.withdraw(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "DRAFT".equals(row.getStatus())));
    }

    @Test
    void withdrawRejectsRunning() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.withdraw(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void startMovesPendingToRunning() {
        ImportTask task = new ImportTask();
        task.setStatus("PENDING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.start(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "RUNNING".equals(row.getStatus())
                        && LocalDateTime.parse("2026-10-16T16:00").equals(row.getStartedAt())));
    }

    @Test
    void startRejectsDraft() {
        ImportTask task = new ImportTask();
        task.setStatus("DRAFT");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.start(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void failIncrementsCount() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(1);
        task.setTotalRows(3);
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportFailureRequest request = new ImportFailureRequest();
        request.setRowNo(4);
        request.setMessage("批号重复");
        request.setErrorCode("430022");
        request.setRawData("L-1,10");

        service.fail(7L, request);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                Integer.valueOf(2).equals(row.getFailRows()) && Integer.valueOf(4).equals(row.getTotalRows())));
        verify(errors).save(org.mockito.ArgumentMatchers.<ImportError>argThat(row ->
                Long.valueOf(7L).equals(row.getTaskId())
                        && Integer.valueOf(4).equals(row.getRowNo())
                        && "批号重复".equals(row.getMessage())
                        && "430022".equals(row.getErrorCode())
                        && "L-1,10".equals(row.getRawData())));
    }

    @Test
    void failRejectsMessageOver512() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(1);
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportFailureRequest request = new ImportFailureRequest();
        request.setRowNo(4);
        request.setMessage("错".repeat(513));

        assertThatThrownBy(() -> service.fail(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(errors, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failRejectsRawDataOver1024() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportFailureRequest request = new ImportFailureRequest();
        request.setRowNo(4);
        request.setMessage("批号重复");
        request.setRawData("行".repeat(1025));

        assertThatThrownBy(() -> service.fail(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(errors, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failRejectsErrorCodeOver32() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportFailureRequest request = new ImportFailureRequest();
        request.setRowNo(4);
        request.setMessage("批号重复");
        request.setErrorCode("E".repeat(33));

        assertThatThrownBy(() -> service.fail(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(errors, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failRejectsDuplicateRowNo() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(1);
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        when(errors.exists(7L, 4)).thenReturn(true);
        ImportFailureRequest request = new ImportFailureRequest();
        request.setRowNo(4);
        request.setMessage("批号重复");

        assertThatThrownBy(() -> service.fail(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(errors, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failRejectsPending() {
        ImportTask task = new ImportTask();
        task.setStatus("PENDING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportFailureRequest request = new ImportFailureRequest();
        request.setRowNo(1);
        request.setMessage("批号重复");

        assertThatThrownBy(() -> service.fail(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void succeedIncrementsCount() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setSuccessRows(1);
        task.setTotalRows(1);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.succeed(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                Integer.valueOf(2).equals(row.getSuccessRows()) && Integer.valueOf(2).equals(row.getTotalRows())));
    }

    @Test
    void succeedRejectsPending() {
        ImportTask task = new ImportTask();
        task.setStatus("PENDING");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.succeed(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void finishedRejectsSuccessRow() {
        ImportTask task = new ImportTask();
        task.setStatus("SUCCESS");
        task.setSuccessRows(3);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.succeed(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void finishedRejectsFailureRow() {
        ImportTask task = new ImportTask();
        task.setStatus("PARTIAL");
        task.setFailRows(1);
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportFailureRequest request = new ImportFailureRequest();
        request.setRowNo(2);
        request.setMessage("批号重复");

        assertThatThrownBy(() -> service.fail(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(errors, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void succeedRejectsValidateOnly() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setSuccessRows(0);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.succeed(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void finishWithNoFailuresIsSuccess() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(0);
        task.setSuccessRows(3);
        task.setTotalRows(3);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.finish(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "SUCCESS".equals(row.getStatus())
                        && Integer.valueOf(3).equals(row.getTotalRows())
                        && Integer.valueOf(3).equals(row.getSuccessRows())
                        && LocalDateTime.parse("2026-10-16T16:00").equals(row.getFinishedAt())));
    }

    @Test
    void finishWithFailuresIsPartial() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(2);
        task.setSuccessRows(3);
        task.setTotalRows(5);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.finish(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "PARTIAL".equals(row.getStatus())
                        && Integer.valueOf(5).equals(row.getTotalRows())
                        && Integer.valueOf(3).equals(row.getSuccessRows())));
    }

    @Test
    void finishAllFailedIsFailed() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(2);
        task.setSuccessRows(0);
        task.setTotalRows(2);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.finish(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "FAILED".equals(row.getStatus())
                        && Integer.valueOf(2).equals(row.getTotalRows())
                        && Integer.valueOf(0).equals(row.getSuccessRows())));
    }

    @Test
    void finishRollbackWithFailuresIsFailed() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setOnError("ROLLBACK");
        task.setFailRows(2);
        task.setSuccessRows(3);
        task.setTotalRows(5);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.finish(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "FAILED".equals(row.getStatus())
                        && Integer.valueOf(5).equals(row.getTotalRows())
                        && Integer.valueOf(3).equals(row.getSuccessRows())));
    }

    @Test
    void finishRejectsRowCountMismatch() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(1);
        task.setSuccessRows(1);
        task.setTotalRows(3);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.finish(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void finishEmptyTaskIsSuccess() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setFailRows(0);
        task.setSuccessRows(0);
        task.setTotalRows(0);
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        service.finish(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "SUCCESS".equals(row.getStatus())
                        && Integer.valueOf(0).equals(row.getTotalRows())
                        && LocalDateTime.parse("2026-10-16T16:00").equals(row.getFinishedAt())));
    }

    @Test
    void finishRejectsAlreadyFinished() {
        ImportTask task = new ImportTask();
        task.setStatus("SUCCESS");
        task.setFailRows(0);
        task.setSuccessRows(0);
        task.setTotalRows(0);
        task.setFinishedAt(LocalDateTime.parse("2026-10-16T16:00"));
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.finish(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void finishRejectsCancelled() {
        ImportTask task = new ImportTask();
        task.setStatus("CANCELLED");
        when(repository.findById(7L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.finish(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listsFailures() {
        ImportTask task = new ImportTask();
        task.setStatus("PARTIAL");
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportError error = new ImportError();
        error.setId(15L);
        error.setTaskId(7L);
        error.setRowNo(4);
        error.setMessage("批号重复");
        error.setErrorCode("430022");
        error.setRawData("L-1,10");
        when(errors.listByTask(7L)).thenReturn(List.of(error));

        List<ImportErrorResponse> rows = service.listFailures(7L);

        assertThat(rows).containsExactly(new ImportErrorResponse(15L, 7L, 4, "批号重复", "430022", "L-1,10"));
    }

    @Test
    void listsFailuresByRowNo() {
        ImportTask task = new ImportTask();
        task.setStatus("PARTIAL");
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        ImportError later = new ImportError();
        later.setId(16L);
        later.setTaskId(7L);
        later.setRowNo(4);
        later.setMessage("批号重复");
        ImportError earlier = new ImportError();
        earlier.setId(15L);
        earlier.setTaskId(7L);
        earlier.setRowNo(2);
        earlier.setMessage("数量不合法");
        when(errors.listByTask(7L)).thenReturn(List.of(later, earlier));

        assertThat(service.listFailures(7L)).containsExactly(
                new ImportErrorResponse(15L, 7L, 2, "数量不合法", null, null),
                new ImportErrorResponse(16L, 7L, 4, "批号重复", null, null));
    }

    @Test
    void showsFailureById() {
        ImportError error = new ImportError();
        error.setId(15L);
        error.setTaskId(7L);
        error.setRowNo(4);
        error.setMessage("批号重复");
        error.setErrorCode("430022");
        error.setRawData("L-1,10");
        when(errors.findById(15L)).thenReturn(Optional.of(error));

        assertThat(service.showFailure(15L))
                .isEqualTo(new ImportErrorResponse(15L, 7L, 4, "批号重复", "430022", "L-1,10"));
    }

    @Test
    void missingFailureRejected() {
        when(errors.findById(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.showFailure(15L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void showsByTaskNo() {
        ImportTask task = new ImportTask();
        task.setId(7L);
        task.setTaskNo("IMP-1");
        task.setImportType("WIP_INIT");
        task.setMode("COMMIT");
        task.setOnError("SKIP");
        task.setStatus("PARTIAL");
        task.setTotalRows(5);
        task.setSuccessRows(3);
        task.setFailRows(2);
        task.setFileRef("files/wip.xlsx");
        task.setOperator("zhang");
        task.setStartedAt(LocalDateTime.parse("2026-10-16T16:00"));
        task.setFinishedAt(LocalDateTime.parse("2026-10-16T16:05"));
        when(repository.findByNo("IMP-1")).thenReturn(Optional.of(task));

        ImportTaskResponse shown = service.show("IMP-1");

        assertThat(shown).isEqualTo(new ImportTaskResponse(7L, "IMP-1", "WIP_INIT", "COMMIT", "SKIP", "PARTIAL", 5, 3, 2, "files/wip.xlsx", "zhang",
                LocalDateTime.parse("2026-10-16T16:00"), LocalDateTime.parse("2026-10-16T16:05")));
    }

    @Test
    void showRejectsTaskNoOver64() {
        assertThatThrownBy(() -> service.show("T".repeat(65)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void listsRunningTasks() {
        ImportTask task = new ImportTask();
        task.setId(7L);
        task.setTaskNo("IMP-1");
        task.setImportType("WIP_INIT");
        task.setMode("COMMIT");
        task.setOnError("ROLLBACK");
        task.setStatus("RUNNING");
        task.setTotalRows(2);
        task.setSuccessRows(1);
        task.setFailRows(1);
        task.setStartedAt(LocalDateTime.parse("2026-10-16T16:00"));
        when(repository.listByStatus("RUNNING")).thenReturn(List.of(task));

        assertThat(service.listRunning()).containsExactly(
                new ImportTaskResponse(7L, "IMP-1", "WIP_INIT", "COMMIT", "ROLLBACK", "RUNNING", 2, 1, 1, null, null,
                LocalDateTime.parse("2026-10-16T16:00"), null));
    }

    @Test
    void listsByStatus() {
        ImportTask task = new ImportTask();
        task.setId(8L);
        task.setTaskNo("IMP-2");
        task.setImportType("MATERIAL");
        task.setMode("VALIDATE_ONLY");
        task.setOnError("SKIP");
        task.setStatus("PENDING");
        when(repository.listByStatus("PENDING")).thenReturn(List.of(task));

        assertThat(service.listByStatus("PENDING")).containsExactly(
                new ImportTaskResponse(8L, "IMP-2", "MATERIAL", "VALIDATE_ONLY", "SKIP", "PENDING", 0, 0, 0, null, null, null, null));
    }

    @Test
    void listsByStatusAndType() {
        ImportTask task = new ImportTask();
        task.setId(9L);
        task.setTaskNo("IMP-4");
        task.setImportType("WIP_INIT");
        task.setMode("COMMIT");
        task.setOnError("SKIP");
        task.setStatus("PENDING");
        when(repository.listByStatusAndType("PENDING", "WIP_INIT")).thenReturn(List.of(task));

        assertThat(service.listByStatus("PENDING", "WIP_INIT")).containsExactly(
                new ImportTaskResponse(9L, "IMP-4", "WIP_INIT", "COMMIT", "SKIP", "PENDING", 0, 0, 0, null, null, null, null));
    }

    @Test
    void rejectsUnknownTypeWhenListingByStatus() {
        assertThatThrownBy(() -> service.listByStatus("PENDING", "LOT"))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void rejectsUnknownStatus() {
        assertThatThrownBy(() -> service.listByStatus("DONE"))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void listsByType() {
        ImportTask task = new ImportTask();
        task.setId(10L);
        task.setTaskNo("IMP-3");
        task.setImportType("WIP_INIT");
        task.setMode("COMMIT");
        task.setOnError("SKIP");
        task.setStatus("DRAFT");
        when(repository.listByType("WIP_INIT")).thenReturn(List.of(task));

        assertThat(service.listByType("WIP_INIT")).containsExactly(
                new ImportTaskResponse(10L, "IMP-3", "WIP_INIT", "COMMIT", "SKIP", "DRAFT", 0, 0, 0, null, null, null, null));
    }

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(() -> service.listByType("LOT"))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void listsByOperator() {
        ImportTask task = new ImportTask();
        task.setId(11L);
        task.setTaskNo("IMP-5");
        task.setImportType("WIP_INIT");
        task.setMode("COMMIT");
        task.setOnError("SKIP");
        task.setStatus("DRAFT");
        task.setOperator("zhang");
        when(repository.listByOperator("zhang")).thenReturn(List.of(task));

        assertThat(service.listByOperator("zhang")).containsExactly(
                new ImportTaskResponse(11L, "IMP-5", "WIP_INIT", "COMMIT", "SKIP", "DRAFT", 0, 0, 0, null, "zhang", null,
                        null));
    }

    @Test
    void listsByOperatorAndStatus() {
        ImportTask task = new ImportTask();
        task.setId(12L);
        task.setTaskNo("IMP-5");
        task.setImportType("WIP_INIT");
        task.setMode("COMMIT");
        task.setOnError("SKIP");
        task.setStatus("RUNNING");
        task.setOperator("zhang");
        when(repository.listByOperatorAndStatus("zhang", "RUNNING")).thenReturn(List.of(task));

        assertThat(service.listByOperator("zhang", "RUNNING")).containsExactly(
                new ImportTaskResponse(12L, "IMP-5", "WIP_INIT", "COMMIT", "SKIP", "RUNNING", 0, 0, 0, null, "zhang", null,
                        null));
    }

    @Test
    void rejectsUnknownStatusWhenListingByOperator() {
        assertThatThrownBy(() -> service.listByOperator("zhang", "DONE"))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void rejectsBlankOperator() {
        assertThatThrownBy(() -> service.listByOperator(" "))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
    }

    @Test
    void createsWithOperator() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setOperator("zhang");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "zhang".equals(row.getOperator())));
    }

    @Test
    void createsWithFileRef() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setFileRef("files/wip.xlsx");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<ImportTask>argThat(row ->
                "files/wip.xlsx".equals(row.getFileRef())));
    }

    @Test
    void rejectsFileRefOver256() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setFileRef("f".repeat(257));

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsOperatorOver64() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("IMP-1");
        request.setImportType("WIP_INIT");
        request.setOperator("张".repeat(65));

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsTaskNoOver64() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo("T".repeat(65));
        request.setImportType("WIP_INIT");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsBlankTaskNo() {
        ImportTaskCreateRequest request = new ImportTaskCreateRequest();
        request.setTaskNo(" ");
        request.setImportType("WIP_INIT");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.IMPORT_TASK_INVALID);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
