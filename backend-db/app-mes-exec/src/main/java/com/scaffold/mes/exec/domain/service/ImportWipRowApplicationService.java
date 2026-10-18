package com.scaffold.mes.exec.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.imports.ImportFailureRequest;
import com.scaffold.mes.core.domain.entity.imports.ImportTask;
import com.scaffold.mes.core.domain.repository.imports.ImportTaskRepository;
import com.scaffold.mes.core.domain.service.imports.ImportTaskApplicationService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.ImportWipRowRequest;
import com.scaffold.mes.exec.domain.dto.WipImportRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.provider.TimeLinkProvider;

import lombok.RequiredArgsConstructor;

/**
 * ImportWipRowApplicationService 按导入任务写一行在制
 * <p>
 * VALIDATE_ONLY 不写 Lot。提交写入且出错回滚时，时间链为 0 且没填计时起点，记成时间链和计时起点要一起填，并抛出同样的错，不写 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ImportWipRowApplicationService {

    private final ImportTaskRepository tasks;

    private final ImportTaskApplicationService taskService;

    private final LotApplicationService lots;

    private final LotRepository lotRepository;

    private final WorkOrderRepository workOrders;

    private final RouteNextProvider routeNext;

    private final WaferRepository wafers;

    private final HoldReasonRepository holdReasons;

    private final TimeLinkProvider timeLinks;

    /**
     * 写入一行在制。
     * <p>
     * 本方法不包事务。Lot 写入失败时其事务回滚，失败行由任务服务另起事务记下。
     *
     * @param taskId 任务
     * @param request 工单和这一行
     * @throws BusinessException 任务不能写入；ROLLBACK 时这一行写入失败也会抛出
     */
    public void importRow(Long taskId, ImportWipRowRequest request) {
        ImportTask task = tasks.findById(taskId)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "导入任务不存在"));
        if (!"RUNNING".equals(task.getStatus()) || !"WIP_INIT".equals(task.getImportType())
                || (!"COMMIT".equals(task.getMode()) && !"VALIDATE_ONLY".equals(task.getMode()))) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "只有运行中的在制提交任务可以写入");
        }
        if (request.getRowNo() == null || request.getRowNo() < 1) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "失败行号和错误信息不能为空");
        }
        if ("VALIDATE_ONLY".equals(task.getMode())) {
            validateOnly(taskId, request);
            return;
        }
        if (request.getRow() != null && request.getRow().getStepEnterAt() != null
                && request.getRow().getLastTrackOutAt() != null
                && request.getRow().getStepEnterAt().isBefore(request.getRow().getLastTrackOutAt())) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "进站时间不能早于上次出站时间",
                    request.getRow().getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "进站时间不能早于上次出站时间");
            }
            return;
        }
        WipImportRequest writing = request.getRow();
        if (writing == null || writing.getQty() == null || writing.getQty() < 1) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_QTY_INVALID, "开批数量必须大于 0",
                    writing == null ? null : writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.LOT_QTY_INVALID, "开批数量必须大于 0");
            }
            return;
        }
        WorkOrder workOrder = workOrders.findById(request.getWoId()).orElse(null);
        if (workOrder == null) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在",
                    writing == null ? null : writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在");
            }
            return;
        }
        if (!"RELEASED".equals(workOrder.getStatus())) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_WO_NOT_RELEASED, "只有已下达的工单可以开批",
                    writing == null ? null : writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.LOT_WO_NOT_RELEASED, "只有已下达的工单可以开批");
            }
            return;
        }
        if (writing.getLotNo() == null || writing.getLotNo().isBlank()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_NO_REQUIRED, "批号不能为空", null);
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.LOT_NO_REQUIRED, "批号不能为空");
            }
            return;
        }
        if (writing.getSeqNo() == null || writing.getSeqNo() < 1 || writing.getStepId() == null) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "期初导入必须指定站点",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "期初导入必须指定站点");
            }
            return;
        }
        if (routeNext.at(workOrder.getRouteVersionId(), writing.getSeqNo())
                .filter(step -> writing.getStepId().equals(step.stepId()))
                .isEmpty()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "当前站点不在工单工艺路线上",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "当前站点不在工单工艺路线上");
            }
            return;
        }
        int passCount = writing.getPassCount() == null ? 0 : writing.getPassCount();
        if (passCount < 0) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "经过次数不能小于 0",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "经过次数不能小于 0");
            }
            return;
        }
        boolean priorStep = writing.getPriorStepId() != null;
        boolean priorCount = writing.getPriorPassCount() != null;
        if (priorStep != priorCount || (priorCount && writing.getPriorPassCount() < 1)
                || (priorStep && writing.getPriorStepId().equals(writing.getStepId()))) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "已经过站点要单独指定，次数至少 1",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "已经过站点要单独指定，次数至少 1");
            }
            return;
        }
        boolean rework = writing.getReworkId() != null;
        boolean reworkSeq = writing.getReworkSeqNo() != null;
        if (rework != reworkSeq || (reworkSeq && writing.getReworkSeqNo() < 1)) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "返工单和返工步骤序号要一起填，序号至少 1",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "返工单和返工步骤序号要一起填，序号至少 1");
            }
            return;
        }
        if (writing.getLocationId() != null && writing.getLocationId() < 1) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "库位不合法", writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "库位不合法");
            }
            return;
        }
        if (writing.getRecipeVersionId() != null && writing.getRecipeVersionId() < 1) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "Recipe 版本不合法",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "Recipe 版本不合法");
            }
            return;
        }
        boolean link = writing.getTimeLinkId() != null;
        boolean linkStart = writing.getTimeLinkStartAt() != null;
        if (link != linkStart) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "时间链和计时起点要一起填",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "时间链和计时起点要一起填");
            }
            return;
        }
        if (link && timeLinks.find(workOrder.getRouteVersionId(), writing.getTimeLinkId())
                .filter(row -> row.maxMinutes() != null && row.maxMinutes() >= 1 && row.toSeqNo() != null)
                .isEmpty()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "时间链不在这个工艺上",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "时间链不在这个工艺上");
            }
            return;
        }
        boolean wafer = writing.getWaferCode() != null && !writing.getWaferCode().isBlank();
        boolean slot = writing.getSlotNo() != null;
        if (wafer != slot || (slot && writing.getSlotNo() < 1)) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "Wafer 编号和槽位要一起填，槽位至少 1",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "Wafer 编号和槽位要一起填，槽位至少 1");
            }
            return;
        }
        if (wafer && wafers.findByCode(writing.getWaferCode()).isPresent()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WAFER_CODE_EXISTS, "Wafer 编号已存在",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WAFER_CODE_EXISTS, "Wafer 编号已存在");
            }
            return;
        }
        String waferStatus = writing.getWaferStatus() == null || writing.getWaferStatus().isBlank()
                ? "NORMAL" : writing.getWaferStatus();
        if (wafer && !"NORMAL".equals(waferStatus) && !"SCRAPPED".equals(waferStatus)) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "Wafer 状态只能是 NORMAL 或 SCRAPPED",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "Wafer 状态只能是 NORMAL 或 SCRAPPED");
            }
            return;
        }
        if (wafer && "SCRAPPED".equals(waferStatus)
                && (writing.getScrapReason() == null || writing.getScrapReason().isBlank())) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "报废必须填写原因码",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "报废必须填写原因码");
            }
            return;
        }
        String holdCode = writing.getHoldReasonCode();
        if (holdCode != null && !holdCode.isBlank()) {
            HoldReason reason = holdReasons.findByCode(holdCode).orElse(null);
            if (reason == null) {
                recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.HOLD_REASON_NOT_FOUND, "Hold 原因不存在",
                        writing.getLotNo());
                if ("ROLLBACK".equals(task.getOnError())) {
                    throw new BusinessException(MesExecErrorCodes.HOLD_REASON_NOT_FOUND, "Hold 原因不存在");
                }
                return;
            }
            if (reason.getEnabled() == null || reason.getEnabled() != 1) {
                recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_HOLD_REASON_DISABLED, "Hold 原因已停用",
                        writing.getLotNo());
                if ("ROLLBACK".equals(task.getOnError())) {
                    throw new BusinessException(MesExecErrorCodes.LOT_HOLD_REASON_DISABLED, "Hold 原因已停用");
                }
                return;
            }
        }
        if (lotRepository.findByNo(writing.getLotNo()).isPresent()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_NO_EXISTS,
                    "Lot 号已存在: " + writing.getLotNo(), writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.LOT_NO_EXISTS, "Lot 号已存在: " + writing.getLotNo());
            }
            return;
        }
        long used = lotRepository.sumInitialQty(request.getWoId());
        if (workOrder.getPlanQty() != null && used + writing.getQty() > workOrder.getPlanQty()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_OVER_PLAN, "开批数量超过工单计划量",
                    writing.getLotNo());
            if ("ROLLBACK".equals(task.getOnError())) {
                throw new BusinessException(MesExecErrorCodes.LOT_OVER_PLAN, "开批数量超过工单计划量");
            }
            return;
        }
        try {
            lots.importWip(request.getWoId(), request.getRow());
        } catch (BusinessException ex) {
            ImportFailureRequest failure = new ImportFailureRequest();
            failure.setRowNo(request.getRowNo());
            failure.setMessage(ex.getMessage());
            failure.setErrorCode(Integer.toString(ex.getCode()));
            failure.setRawData(request.getRow() == null ? null : request.getRow().getLotNo());
            taskService.fail(taskId, failure);
            if ("ROLLBACK".equals(task.getOnError())) {
                throw ex;
            }
            return;
        }
        taskService.succeed(taskId);
    }

    private void validateOnly(Long taskId, ImportWipRowRequest request) {
        String lotNo = request.getRow() == null ? null : request.getRow().getLotNo();
        Long qty = request.getRow() == null ? null : request.getRow().getQty();
        if (qty == null || qty < 1) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_QTY_INVALID, "开批数量必须大于 0", lotNo);
            return;
        }
        WorkOrder workOrder = workOrders.findById(request.getWoId()).orElse(null);
        if (workOrder == null) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在", lotNo);
            return;
        }
        if (!"RELEASED".equals(workOrder.getStatus())) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_WO_NOT_RELEASED, "只有已下达的工单可以开批", lotNo);
            return;
        }
        if (lotNo == null || lotNo.isBlank()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_NO_REQUIRED, "批号不能为空", null);
            return;
        }
        Integer seqNo = request.getRow().getSeqNo();
        Long stepId = request.getRow().getStepId();
        if (seqNo == null || seqNo < 1 || stepId == null) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "期初导入必须指定站点", lotNo);
            return;
        }
        boolean onRoute = routeNext.at(workOrder.getRouteVersionId(), seqNo)
                .filter(step -> stepId.equals(step.stepId()))
                .isPresent();
        if (!onRoute) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "当前站点不在工单工艺路线上", lotNo);
            return;
        }
        int passCount = request.getRow().getPassCount() == null ? 0 : request.getRow().getPassCount();
        if (passCount < 0) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "经过次数不能小于 0", lotNo);
            return;
        }
        boolean priorStep = request.getRow().getPriorStepId() != null;
        boolean priorCount = request.getRow().getPriorPassCount() != null;
        if (priorStep != priorCount || (priorCount && request.getRow().getPriorPassCount() < 1)
                || (priorStep && request.getRow().getPriorStepId().equals(stepId))) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "已经过站点要单独指定，次数至少 1", lotNo);
            return;
        }
        boolean rework = request.getRow().getReworkId() != null;
        boolean reworkSeq = request.getRow().getReworkSeqNo() != null;
        if (rework != reworkSeq || (reworkSeq && request.getRow().getReworkSeqNo() < 1)) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "返工单和返工步骤序号要一起填，序号至少 1", lotNo);
            return;
        }
        if (request.getRow().getLocationId() != null && request.getRow().getLocationId() < 1) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "库位不合法", lotNo);
            return;
        }
        if (request.getRow().getRecipeVersionId() != null && request.getRow().getRecipeVersionId() < 1) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "Recipe 版本不合法", lotNo);
            return;
        }
        boolean link = request.getRow().getTimeLinkId() != null;
        boolean linkStart = request.getRow().getTimeLinkStartAt() != null;
        if (link != linkStart) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "时间链和计时起点要一起填", lotNo);
            return;
        }
        if (link && timeLinks.find(workOrder.getRouteVersionId(), request.getRow().getTimeLinkId())
                .filter(row -> row.maxMinutes() != null && row.maxMinutes() >= 1 && row.toSeqNo() != null)
                .isEmpty()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "时间链不在这个工艺上", lotNo);
            return;
        }
        boolean wafer = request.getRow().getWaferCode() != null && !request.getRow().getWaferCode().isBlank();
        boolean slot = request.getRow().getSlotNo() != null;
        if (wafer != slot || (slot && request.getRow().getSlotNo() < 1)) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "Wafer 编号和槽位要一起填，槽位至少 1", lotNo);
            return;
        }
        if (wafer && wafers.findByCode(request.getRow().getWaferCode()).isPresent()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WAFER_CODE_EXISTS, "Wafer 编号已存在", lotNo);
            return;
        }
        String waferStatus = request.getRow().getWaferStatus() == null || request.getRow().getWaferStatus().isBlank()
                ? "NORMAL" : request.getRow().getWaferStatus();
        if (wafer && !"NORMAL".equals(waferStatus) && !"SCRAPPED".equals(waferStatus)) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "Wafer 状态只能是 NORMAL 或 SCRAPPED", lotNo);
            return;
        }
        if (wafer && "SCRAPPED".equals(waferStatus)
                && (request.getRow().getScrapReason() == null || request.getRow().getScrapReason().isBlank())) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "报废必须填写原因码", lotNo);
            return;
        }
        String holdCode = request.getRow().getHoldReasonCode();
        if (holdCode != null && !holdCode.isBlank()) {
            HoldReason reason = holdReasons.findByCode(holdCode).orElse(null);
            if (reason == null) {
                recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.HOLD_REASON_NOT_FOUND, "Hold 原因不存在", lotNo);
                return;
            }
            if (reason.getEnabled() == null || reason.getEnabled() != 1) {
                recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_HOLD_REASON_DISABLED, "Hold 原因已停用",
                        lotNo);
                return;
            }
        }
        LocalDateTime stepEnterAt = request.getRow().getStepEnterAt();
        LocalDateTime lastTrackOutAt = request.getRow().getLastTrackOutAt();
        if (stepEnterAt != null && lastTrackOutAt != null && stepEnterAt.isBefore(lastTrackOutAt)) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.WIP_STEP, "进站时间不能早于上次出站时间", lotNo);
            return;
        }
        if (lotNo != null && lotRepository.findByNo(lotNo).isPresent()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_NO_EXISTS, "Lot 号已存在: " + lotNo, lotNo);
            return;
        }
        long used = lotRepository.sumInitialQty(request.getWoId());
        if (workOrder.getPlanQty() != null && used + qty > workOrder.getPlanQty()) {
            recordFailure(taskId, request.getRowNo(), MesExecErrorCodes.LOT_OVER_PLAN, "开批数量超过工单计划量", lotNo);
        }
    }

    private void recordFailure(Long taskId, Integer rowNo, int code, String message, String raw) {
        ImportFailureRequest failure = new ImportFailureRequest();
        failure.setRowNo(rowNo);
        failure.setMessage(message);
        failure.setErrorCode(Integer.toString(code));
        failure.setRawData(raw);
        taskService.fail(taskId, failure);
    }

    /**
     * 一次写入多行。每一行单独记成功或失败。
     *
     * @param taskId 任务
     * @param rows 各行
     * @throws BusinessException 没有行；ROLLBACK 时某一行写入失败也会抛出并停在这一行
     */
    public void importRows(Long taskId, List<ImportWipRowRequest> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(MesErrorCodes.IMPORT_TASK_INVALID, "至少要有一行");
        }
        for (ImportWipRowRequest row : rows) {
            importRow(taskId, row);
        }
    }
}
