package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.imports.ImportFailureRequest;
import com.scaffold.mes.core.domain.entity.imports.ImportTask;
import com.scaffold.mes.core.domain.repository.imports.ImportTaskRepository;
import com.scaffold.mes.core.domain.service.imports.ImportTaskApplicationService;
import com.scaffold.mes.exec.domain.dto.ImportWipRowRequest;
import com.scaffold.mes.exec.domain.dto.WipImportRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.provider.TimeLinkProvider;

/**
 * VALIDATE_ONLY 不写 Lot。提交写入且出错回滚时，时间链为 0 且没填计时起点，记成时间链和计时起点要一起填，并抛出同样的错，不写 Lot。
 */
class ImportWipRowApplicationServiceTest {

    private ImportTaskRepository tasks;
    private ImportTaskApplicationService taskService;
    private LotApplicationService lots;
    private LotRepository lotRepository;
    private WorkOrderRepository workOrders;
    private RouteNextProvider routeNext;
    private WaferRepository wafers;
    private HoldReasonRepository holdReasons;
    private TimeLinkProvider timeLinks;
    private ImportWipRowApplicationService service;

    @BeforeEach
    void setUp() {
        tasks = mock(ImportTaskRepository.class);
        taskService = mock(ImportTaskApplicationService.class);
        lots = mock(LotApplicationService.class);
        lotRepository = mock(LotRepository.class);
        workOrders = mock(WorkOrderRepository.class);
        routeNext = mock(RouteNextProvider.class);
        wafers = mock(WaferRepository.class);
        holdReasons = mock(HoldReasonRepository.class);
        timeLinks = mock(TimeLinkProvider.class);
        when(lotRepository.findByNo(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        when(routeNext.at(21L, 2)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(2, 7L, null)));
        service = new ImportWipRowApplicationService(tasks, taskService, lots, lotRepository, workOrders, routeNext,
                wafers, holdReasons, timeLinks);
    }

    @Test
    void commitWritesLotAndCountsSuccess() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
    }

    @Test
    void commitWritesLotWhenStepEnterNotBeforeLastTrackOut() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setStepEnterAt(LocalDateTime.parse("2026-10-16T17:00"));
        row.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T16:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsFailureWhenStepOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 8L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "当前站点不在工单工艺路线上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenStepOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsMissingWorkOrder() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430011".equals(failure.getErrorCode())
                                && "工单不存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsUnreleasedWorkOrder() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder created = new WorkOrder();
        created.setStatus("CREATED");
        created.setRouteVersionId(21L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(created));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430024".equals(failure.getErrorCode())
                                && "只有已下达的工单可以开批".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsQtyBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430023".equals(failure.getErrorCode())
                                && "开批数量必须大于 0".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsMissingQty() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430023".equals(failure.getErrorCode())
                                && "开批数量必须大于 0".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsBlankLotNo() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setQty(1L);
        row.setLotNo("  ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430027".equals(failure.getErrorCode())
                                && "批号不能为空".equals(failure.getMessage())
                                && failure.getRawData() == null));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsMissingStep() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "期初导入必须指定站点".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsSeqBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(0);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "期初导入必须指定站点".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsSeqWithoutStep() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "期初导入必须指定站点".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsStepWithoutSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "期初导入必须指定站点".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativePassCount() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPassCount(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "经过次数不能小于 0".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenPassCountMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenPassCountZero() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPassCount(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPriorStepWithoutCount() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPriorStepId(4L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "已经过站点要单独指定，次数至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPriorPassCountBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPriorStepId(4L);
        row.setPriorPassCount(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "已经过站点要单独指定，次数至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPriorStepSameAsCurrent() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPriorStepId(7L);
        row.setPriorPassCount(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "已经过站点要单独指定，次数至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenPriorStepDiffers() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPriorStepId(4L);
        row.setPriorPassCount(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPriorCountWithoutStep() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPriorPassCount(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "已经过站点要单独指定，次数至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsReworkWithoutSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(6L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsReworkSeqBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(6L);
        row.setReworkSeqNo(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeReworkSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(6L);
        row.setReworkSeqNo(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeReworkSeqWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkSeqNo(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsReworkSeqZeroWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkSeqNo(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsReworkSeqZeroWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkSeqNo(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "返工单和返工步骤序号要一起填，序号至少 1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsReworkSeqZero() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(6L);
        row.setReworkSeqNo(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "返工单和返工步骤序号要一起填，序号至少 1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsNegativeReworkSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(6L);
        row.setReworkSeqNo(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "返工单和返工步骤序号要一起填，序号至少 1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsNegativeReworkSeqWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkSeqNo(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "返工单和返工步骤序号要一起填，序号至少 1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsReworkWithoutSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(6L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "返工单和返工步骤序号要一起填，序号至少 1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsReworkSeqWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkSeqNo(2);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "返工单和返工步骤序号要一起填，序号至少 1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenReworkPairValid() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(6L);
        row.setReworkSeqNo(2);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsReworkSeqWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkSeqNo(2);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsLocationBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setLocationId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "库位不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsLocationZero() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setLocationId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "库位不合法");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "库位不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeLocation() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setLocationId(-1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "库位不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsNegativeLocation() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setLocationId(-1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "库位不合法");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "库位不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenLocationAtLeastOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setLocationId(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsRecipeVersionBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setRecipeVersionId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Recipe 版本不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsRecipeVersionZero() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setRecipeVersionId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "Recipe 版本不合法");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Recipe 版本不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeRecipeVersion() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setRecipeVersionId(-1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Recipe 版本不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsNegativeRecipeVersion() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setRecipeVersionId(-1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "Recipe 版本不合法");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Recipe 版本不合法".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenRecipeVersionAtLeastOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setRecipeVersionId(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "时间链和计时起点要一起填");
        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, -1L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(-1L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(-1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsNegativeTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, -1L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(-1L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(-1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "时间链和计时起点要一起填");
        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsZeroTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 0L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(0L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsZeroTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 0L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(0L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "时间链和计时起点要一起填");
        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkStartWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsTimeLinkStartWithoutId() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode(), Throwable::getMessage)
                .containsExactly(430493, "时间链和计时起点要一起填");
        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, -1L)).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(-1L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, -1L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsZeroTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, 0L)).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(0L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 0L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenZeroTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        when(timeLinks.find(21L, 0L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(0L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(0L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 0L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenNegativeTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        when(timeLinks.find(21L, -1L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(-1L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(-1L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, -1L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeTimeLinkMaxMinutesBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, -1L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(-1L, 1, 2, 0, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(-1L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, -1L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeTimeLinkWithoutToSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, -1L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(-1L, 1, null, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(-1L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, -1L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeTimeLinkMaxMinutesMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, -1L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(-1L, 1, 2, null, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(-1L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, -1L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsZeroTimeLinkMaxMinutesMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, 0L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(0L, 1, 2, null, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(0L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 0L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsZeroTimeLinkWithoutToSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, 0L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(0L, 1, null, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(0L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 0L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsZeroTimeLinkMaxMinutesBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(timeLinks.find(21L, 0L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(0L, 1, 2, 0, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(0L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 0L);
        verify(timeLinks, never()).find(21L, 5L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkMaxMinutesBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 0, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkMaxMinutesIsOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 1, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkMaxOneWithoutToSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, null, 1, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkToSeqZero() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 0, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkToSeqNegative() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, -1, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkFromSeqNegative() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, -1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkFromSeqZero() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 0, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkFromSeqMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, null, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkActionBlank() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "  ", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkReasonBlank() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "HOLD", "  ")));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkActionMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, null, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkActionUnknown() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "STOP", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkActionLowerHold() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "hold", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkActionLowerWarn() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "warn", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkActionPadded() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, " WARN ", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkReasonUnknown() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "HOLD", "GONE")));
        when(holdReasons.findByCode("GONE")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkReasonUnknownAndHoldEnabled() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "HOLD", "GONE")));
        when(holdReasons.findByCode("GONE")).thenReturn(Optional.empty());
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(holdReasons).findByCode("EQ");
        verify(holdReasons, never()).findByCode("GONE");
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkReasonUnknownAndHoldDisabled() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "HOLD", "GONE")));
        when(holdReasons.findByCode("GONE")).thenReturn(Optional.empty());
        HoldReason reason = new HoldReason();
        reason.setEnabled(0);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(holdReasons).findByCode("EQ");
        verify(holdReasons, never()).findByCode("GONE");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430051".equals(failure.getErrorCode())
                                && "Hold 原因已停用".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkReasonUnknownAndHoldMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "HOLD", "GONE")));
        when(holdReasons.findByCode("GONE")).thenReturn(Optional.empty());
        when(holdReasons.findByCode("MISS")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("MISS");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(holdReasons).findByCode("MISS");
        verify(holdReasons, never()).findByCode("GONE");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430001".equals(failure.getErrorCode())
                                && "Hold 原因不存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldEnabledAndTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldDisabledAndTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        HoldReason reason = new HoldReason();
        reason.setEnabled(0);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldMissingAndTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        when(holdReasons.findByCode("MISS")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("MISS");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenHoldEnabledAndTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons).findByCode("EQ");
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldDisabledAndTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        HoldReason reason = new HoldReason();
        reason.setEnabled(0);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons).findByCode("EQ");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430051".equals(failure.getErrorCode())
                                && "Hold 原因已停用".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldMissingAndTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(holdReasons.findByCode("MISS")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("MISS");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons).findByCode("MISS");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430001".equals(failure.getErrorCode())
                                && "Hold 原因不存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenHoldBlankAndTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        when(holdReasons.findByCode("  ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("  ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldBlankAndTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        when(holdReasons.findByCode("  ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("  ");
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks).find(21L, 5L);
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldBlankAndTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(holdReasons.findByCode("  ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("  ");
        row.setTimeLinkId(5L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldEnabledAndTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldDisabledAndTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(0);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkId(5L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldMissingAndTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(holdReasons.findByCode("MISS")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("MISS");
        row.setTimeLinkId(5L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldEnabledAndTimeLinkStartOnly() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldDisabledAndTimeLinkStartOnly() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(0);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldMissingAndTimeLinkStartOnly() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(holdReasons.findByCode("MISS")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("MISS");
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldBlankAndTimeLinkStartOnly() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(holdReasons.findByCode("  ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("  ");
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(timeLinks, never()).find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(holdReasons, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkWithoutToSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, null, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsTimeLinkMaxMinutesMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, null, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsWaferWithoutSlot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsSlotBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeSlot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNegativeSlotWithoutWaferCode() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setSlotNo(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsBlankWaferCodeWithNegativeSlot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("  ");
        row.setSlotNo(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsBlankWaferCodeWithZeroSlot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("  ");
        row.setSlotNo(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsZeroSlotWithoutWaferCode() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setSlotNo(0);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsSlotWithoutWaferCode() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsBlankWaferCodeWithSlot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("  ");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenWaferCodeBlankAndSlotMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("  ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsNullWaferCodeWithSlot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode(null);
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers, never()).findByCode(org.mockito.ArgumentMatchers.any());
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsDuplicateWaferCode() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.of(new Wafer()));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430041".equals(failure.getErrorCode())
                                && "Wafer 编号已存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenWaferCodeFree() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenWaferCodeCaseDiffers() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.of(new Wafer()));
        when(wafers.findByCode("w-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("w-1");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers).findByCode("w-1");
        verify(wafers, never()).findByCode("W-1");
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsLowerWaferCodeExists() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("w-1")).thenReturn(Optional.of(new Wafer()));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("w-1");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers).findByCode("w-1");
        verify(wafers, never()).findByCode("W-1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430041".equals(failure.getErrorCode())
                                && "Wafer 编号已存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenPaddedWaferCodeFree() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.of(new Wafer()));
        when(wafers.findByCode(" W-1 ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode(" W-1 ");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers).findByCode(" W-1 ");
        verify(wafers, never()).findByCode("W-1");
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPaddedWaferCodeExists() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode(" W-1 ")).thenReturn(Optional.of(new Wafer()));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode(" W-1 ");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(wafers).findByCode(" W-1 ");
        verify(wafers, never()).findByCode("W-1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430041".equals(failure.getErrorCode())
                                && "Wafer 编号已存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsBadWaferStatus() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("HOLD");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 状态只能是 NORMAL 或 SCRAPPED".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenWaferStatusBlank() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("  ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPaddedWaferStatus() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus(" NORMAL ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 状态只能是 NORMAL 或 SCRAPPED".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenWaferStatusNormal() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("NORMAL");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsLowerWaferStatus() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("normal");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 状态只能是 NORMAL 或 SCRAPPED".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsLowerScrapStatus() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("scrapped");
        row.setScrapReason("BROKEN");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 状态只能是 NORMAL 或 SCRAPPED".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPaddedScrapStatus() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus(" SCRAPPED ");
        row.setScrapReason("BROKEN");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 状态只能是 NORMAL 或 SCRAPPED".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsMixedScrapStatus() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(wafers.findByCode("W-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("Scrapped");
        row.setScrapReason("BROKEN");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 状态只能是 NORMAL 或 SCRAPPED".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsScrapWithoutReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("SCRAPPED");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "报废必须填写原因码".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsBlankScrapReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("SCRAPPED");
        row.setScrapReason("  ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "报废必须填写原因码".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenScrapReasonFilled() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("SCRAPPED");
        row.setScrapReason("BROKEN");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsMissingHoldReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(holdReasons.findByCode("GONE")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("GONE");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430001".equals(failure.getErrorCode())
                                && "Hold 原因不存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPaddedHoldReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        when(holdReasons.findByCode(" EQ ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode(" EQ ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(holdReasons).findByCode(" EQ ");
        verify(holdReasons, never()).findByCode("EQ");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430001".equals(failure.getErrorCode())
                                && "Hold 原因不存在".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenPaddedHoldReasonEnabled() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode(" EQ ")).thenReturn(Optional.of(reason));
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode(" EQ ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(holdReasons).findByCode(" EQ ");
        verify(holdReasons, never()).findByCode("EQ");
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPaddedHoldReasonDisabled() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason padded = new HoldReason();
        padded.setEnabled(0);
        HoldReason trimmed = new HoldReason();
        trimmed.setEnabled(1);
        when(holdReasons.findByCode(" EQ ")).thenReturn(Optional.of(padded));
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(trimmed));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode(" EQ ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(holdReasons).findByCode(" EQ ");
        verify(holdReasons, never()).findByCode("EQ");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430051".equals(failure.getErrorCode())
                                && "Hold 原因已停用".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsDisabledHoldReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(0);
        when(holdReasons.findByCode("OLD")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("OLD");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430051".equals(failure.getErrorCode())
                                && "Hold 原因已停用".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldReasonEnabledMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(holdReasons.findByCode("OLD")).thenReturn(Optional.of(new HoldReason()));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("OLD");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430051".equals(failure.getErrorCode())
                                && "Hold 原因已停用".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsHoldReasonEnabledNotOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(2);
        when(holdReasons.findByCode("OLD")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("OLD");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430051".equals(failure.getErrorCode())
                                && "Hold 原因已停用".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenHoldReasonEnabled() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        HoldReason reason = new HoldReason();
        reason.setEnabled(1);
        when(holdReasons.findByCode("EQ")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("EQ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenHoldReasonBlank() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("  ");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsDuplicateLotNo() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.of(new Lot()));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430022".equals(failure.getErrorCode())
                                && "Lot 号已存在: L-1".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenPaddedLotNoFree() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.of(new Lot()));
        when(lotRepository.findByNo(" L-1 ")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo(" L-1 ");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lotRepository).findByNo(" L-1 ");
        verify(lotRepository, never()).findByNo("L-1");
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPaddedLotNoExists() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo(" L-1 ")).thenReturn(Optional.of(new Lot()));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo(" L-1 ");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lotRepository).findByNo(" L-1 ");
        verify(lotRepository, never()).findByNo("L-1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430022".equals(failure.getErrorCode())
                                && "Lot 号已存在:  L-1 ".equals(failure.getMessage())
                                && " L-1 ".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenLotNoCaseDiffers() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.of(new Lot()));
        when(lotRepository.findByNo("l-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("l-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lotRepository).findByNo("l-1");
        verify(lotRepository, never()).findByNo("L-1");
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsLowerLotNoExists() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo("l-1")).thenReturn(Optional.of(new Lot()));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("l-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lotRepository).findByNo("l-1");
        verify(lotRepository, never()).findByNo("L-1");
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430022".equals(failure.getErrorCode())
                                && "Lot 号已存在: l-1".equals(failure.getMessage())
                                && "l-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsQtyOverPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(1L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(0L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(2L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenPlanQtyMissing() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(100L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(2L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenQtyEqualsPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(5L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(3L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(2L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsUsedPlusQtyOverPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(5L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(3L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(3L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsUsedEqualsPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(5L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(5L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsUsedAbovePlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(5L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(6L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPlanQtyZero() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(0L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(0L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsPlanQtyNegative() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(-1L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(0L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenOtherWorkOrderUsedIsLarge() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(5L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(org.mockito.ArgumentMatchers.anyLong())).thenReturn(1000L);
        when(lotRepository.sumInitialQty(4L)).thenReturn(0L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lotRepository).sumInitialQty(4L);
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsThisWorkOrderUsedOverPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(5L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(org.mockito.ArgumentMatchers.anyLong())).thenReturn(0L);
        when(lotRepository.sumInitialQty(4L)).thenReturn(5L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lotRepository).sumInitialQty(4L);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitWritesLotWhenThisWorkOrderUsedEqualsPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(5L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(org.mockito.ArgumentMatchers.anyLong())).thenReturn(1000L);
        when(lotRepository.sumInitialQty(4L)).thenReturn(4L);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lotRepository).sumInitialQty(4L);
        verify(lots).importWip(4L, row);
        verify(taskService).succeed(7L);
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyDoesNotWriteLot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        request.setRow(row);
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyDuplicateLotCountsFailure() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.of(new Lot()));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430022".equals(failure.getErrorCode())
                                && "Lot 号已存在: L-1".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsQtyBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430023".equals(failure.getErrorCode())
                                && "开批数量必须大于 0".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsUnreleasedWorkOrder() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder created = new WorkOrder();
        created.setStatus("CREATED");
        when(workOrders.findById(4L)).thenReturn(Optional.of(created));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430024".equals(failure.getErrorCode())
                                && "只有已下达的工单可以开批".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsMissingWorkOrder() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(workOrders.findById(4L)).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430011".equals(failure.getErrorCode())
                                && "工单不存在".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsQtyOverPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setPlanQty(10L);
        released.setRouteVersionId(21L);
        when(workOrders.findById(4L)).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(8L);
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(3L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430025".equals(failure.getErrorCode())
                                && "开批数量超过工单计划量".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsBlankLotNo() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo(" ");
        row.setQty(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430027".equals(failure.getErrorCode())
                                && "批号不能为空".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsStepOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        when(workOrders.findById(4L)).thenReturn(Optional.of(released));
        when(routeNext.at(21L, 10)).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(10);
        row.setStepId(99L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "当前站点不在工单工艺路线上".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsMissingStep() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "期初导入必须指定站点".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsNegativePassCount() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPassCount(-1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "经过次数不能小于 0".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsPriorStepWithoutCount() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setPriorStepId(8L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "已经过站点要单独指定，次数至少 1".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsReworkWithoutSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setReworkId(3L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "返工单和返工步骤序号要一起填，序号至少 1".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsLocationBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setLocationId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "库位不合法".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsRecipeVersionBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setRecipeVersionId(0L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Recipe 版本不合法".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsTimeLinkWithoutStart() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链和计时起点要一起填".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsTimeLinkOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsTimeLinkMaxMinutesBelowOne() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 0, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsTimeLinkWithoutToSeq() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, null, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "时间链不在这个工艺上".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyAcceptsTimeLinkOnRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        when(timeLinks.find(21L, 5L)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.TimeLinkStart(5L, 1, 2, 30, "WARN", null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setTimeLinkId(5L);
        row.setTimeLinkStartAt(LocalDateTime.parse("2026-10-16T12:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsWaferWithoutSlot() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 编号和槽位要一起填，槽位至少 1".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsDuplicateWaferCode() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        when(wafers.findByCode("W-1")).thenReturn(Optional.of(new Wafer()));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430041".equals(failure.getErrorCode())
                                && "Wafer 编号已存在".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsWaferStatus() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("HOLD");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "Wafer 状态只能是 NORMAL 或 SCRAPPED".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsScrapWithoutReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setWaferCode("W-1");
        row.setSlotNo(1);
        row.setWaferStatus("SCRAPPED");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "报废必须填写原因码".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsMissingHoldReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        when(holdReasons.findByCode("BAD")).thenReturn(Optional.empty());
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("BAD");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430001".equals(failure.getErrorCode())
                                && "Hold 原因不存在".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsDisabledHoldReason() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        HoldReason reason = new HoldReason();
        reason.setEnabled(0);
        when(holdReasons.findByCode("OLD")).thenReturn(Optional.of(reason));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setHoldReasonCode("OLD");
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430051".equals(failure.getErrorCode())
                                && "Hold 原因已停用".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyRejectsStepEnterBeforeLastTrackOut() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        row.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T17:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "进站时间不能早于上次出站时间".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyAcceptsEqualStepEnterAndLastTrackOut() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        row.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T16:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyAcceptsStepEnterAfterLastTrackOut() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setStepEnterAt(LocalDateTime.parse("2026-10-16T17:00"));
        row.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T16:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyAcceptsStepEnterWithoutLastTrackOut() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validateOnlyAcceptsLastTrackOutWithoutStepEnter() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("VALIDATE_ONLY");
        task.setImportType("WIP_INIT");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 7L, null)));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        row.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T16:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).fail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void commitRecordsFailureWhenStepEnterBeforeLastTrackOut() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("SKIP");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        row.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T17:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        service.importRow(7L, request);

        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "进站时间不能早于上次出站时间".equals(failure.getMessage())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rollbackRethrowsAfterRecordingStepEnterFailure() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        row.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T17:00"));
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(430493);
        verify(lots, never()).importWip(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "430493".equals(failure.getErrorCode())
                                && "进站时间不能早于上次出站时间".equals(failure.getMessage())));
        verify(taskService, never()).succeed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void writeFailureCountsFailureNotSuccess() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);
        org.mockito.Mockito.doThrow(new BusinessException(430022, "批号重复"))
                .when(lots).importWip(4L, row);

        assertThatThrownBy(() -> service.importRow(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(430022);
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())
                                && "批号重复".equals(failure.getMessage())
                                && "430022".equals(failure.getErrorCode())
                                && "L-1".equals(failure.getRawData())));
        verify(taskService, never()).succeed(7L);
    }

    @Test
    void skipSwallowsWriteFailure() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("SKIP");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest row = new WipImportRequest();
        row.setLotNo("L-1");
        row.setQty(1L);
        row.setSeqNo(1);
        row.setStepId(7L);
        ImportWipRowRequest request = new ImportWipRowRequest();
        request.setWoId(4L);
        request.setRowNo(4);
        request.setRow(row);
        org.mockito.Mockito.doThrow(new BusinessException(430022, "批号重复"))
                .when(lots).importWip(4L, row);

        service.importRow(7L, request);

        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(4).equals(failure.getRowNo())));
        verify(taskService, never()).succeed(7L);
    }

    @Test
    void batchCountsEachRow() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("SKIP");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(1L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(1);
        good.setStepId(7L);
        good.setQty(1L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);
        org.mockito.Mockito.doThrow(new BusinessException(430022, "批号重复"))
                .when(lots).importWip(4L, bad);

        service.importRows(7L, List.of(first, second));

        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())));
        verify(lots).importWip(4L, good);
        verify(taskService).succeed(7L);
    }

    @Test
    void rollbackBatchStopsAfterFirstFailure() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(1L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(1);
        good.setStepId(7L);
        good.setQty(1L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);
        org.mockito.Mockito.doThrow(new BusinessException(430022, "批号重复"))
                .when(lots).importWip(4L, bad);

        assertThatThrownBy(() -> service.importRows(7L, List.of(first, second)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(430022);
        verify(lots, never()).importWip(4L, good);
        verify(taskService, never()).succeed(7L);
    }

    @Test
    void rollbackBatchStopsAfterIllegalStepEnter() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        bad.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T17:00"));
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        assertThatThrownBy(() -> service.importRows(7L, List.of(first, second)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(430493);
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "进站时间不能早于上次出站时间".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, good);
        verify(taskService, never()).succeed(7L);
    }

    @Test
    void skipBatchWritesLaterRowAfterIllegalStepEnter() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("SKIP");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setStepEnterAt(LocalDateTime.parse("2026-10-16T16:00"));
        bad.setLastTrackOutAt(LocalDateTime.parse("2026-10-16T17:00"));
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(1);
        good.setStepId(7L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        service.importRows(7L, List.of(first, second));

        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "进站时间不能早于上次出站时间".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, bad);
        verify(lots).importWip(4L, good);
        verify(taskService).succeed(7L);
    }

    @Test
    void rollbackBatchStopsAfterStepOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 8L, null)));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(1L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        assertThatThrownBy(() -> service.importRows(7L, List.of(first, second)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(430493);
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "当前站点不在工单工艺路线上".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, good);
        verify(taskService, never()).succeed(7L);
    }

    @Test
    void skipBatchWritesLaterRowAfterStepOffRoute() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("SKIP");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(routeNext.at(21L, 1)).thenReturn(Optional.of(
                new com.scaffold.share.integration.mes.base.response.RouteStepNext(1, 8L, null)));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(1L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(2);
        good.setStepId(7L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        service.importRows(7L, List.of(first, second));

        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "当前站点不在工单工艺路线上".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, bad);
        verify(lots).importWip(4L, good);
        verify(taskService).succeed(7L);
    }

    @Test
    void rollbackBatchStopsAfterQtyOverPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(1L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(0L);
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(2L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(1);
        good.setStepId(7L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        assertThatThrownBy(() -> service.importRows(7L, List.of(first, second)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(430025);
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "开批数量超过工单计划量".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, good);
        verify(taskService, never()).succeed(7L);
    }

    @Test
    void skipBatchWritesLaterRowAfterQtyOverPlan() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("SKIP");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        WorkOrder released = new WorkOrder();
        released.setStatus("RELEASED");
        released.setRouteVersionId(21L);
        released.setPlanQty(1L);
        when(workOrders.findById(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(released));
        when(lotRepository.sumInitialQty(4L)).thenReturn(0L);
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(2L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(1);
        good.setStepId(7L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        service.importRows(7L, List.of(first, second));

        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "开批数量超过工单计划量".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, bad);
        verify(lots).importWip(4L, good);
        verify(taskService).succeed(7L);
    }

    @Test
    void skipBatchWritesLaterRowAfterDuplicateLotNo() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("SKIP");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.of(new Lot()));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(1L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(1);
        good.setStepId(7L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        service.importRows(7L, List.of(first, second));

        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "Lot 号已存在: L-1".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, bad);
        verify(lots).importWip(4L, good);
        verify(taskService).succeed(7L);
    }

    @Test
    void rollbackBatchStopsAfterDuplicateLotNo() {
        ImportTask task = new ImportTask();
        task.setStatus("RUNNING");
        task.setMode("COMMIT");
        task.setImportType("WIP_INIT");
        task.setOnError("ROLLBACK");
        when(tasks.findById(7L)).thenReturn(Optional.of(task));
        when(lotRepository.findByNo("L-1")).thenReturn(Optional.of(new Lot()));
        WipImportRequest bad = new WipImportRequest();
        bad.setLotNo("L-1");
        bad.setQty(1L);
        bad.setSeqNo(1);
        bad.setStepId(7L);
        WipImportRequest good = new WipImportRequest();
        good.setLotNo("L-2");
        good.setQty(1L);
        good.setSeqNo(1);
        good.setStepId(7L);
        ImportWipRowRequest first = new ImportWipRowRequest();
        first.setWoId(4L);
        first.setRowNo(1);
        first.setRow(bad);
        ImportWipRowRequest second = new ImportWipRowRequest();
        second.setWoId(4L);
        second.setRowNo(2);
        second.setRow(good);

        assertThatThrownBy(() -> service.importRows(7L, List.of(first, second)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(430022);
        verify(taskService).fail(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.<ImportFailureRequest>argThat(failure ->
                        Integer.valueOf(1).equals(failure.getRowNo())
                                && "Lot 号已存在: L-1".equals(failure.getMessage())));
        verify(lots, never()).importWip(4L, good);
        verify(taskService, never()).succeed(7L);
    }
}
