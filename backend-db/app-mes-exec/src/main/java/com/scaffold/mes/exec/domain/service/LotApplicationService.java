package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotStartRequest;
import com.scaffold.mes.exec.domain.dto.WipImportRequest;
import com.scaffold.mes.exec.domain.entity.HoldReason;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.entity.LotTimeLinkTimer;
import com.scaffold.mes.exec.domain.repository.LotTimeLinkTimerRepository;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.mes.exec.domain.repository.HoldReasonRepository;
import com.scaffold.mes.exec.domain.repository.LotHoldRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.provider.RouteStartProvider;
import com.scaffold.share.integration.mes.base.provider.TimeLinkProvider;
import com.scaffold.share.integration.mes.base.response.TimeLinkStart;
import com.scaffold.share.integration.mes.base.response.RouteStepStart;

import lombok.RequiredArgsConstructor;

/**
 * LotApplicationService Lot 应用服务
 * <p>
 * 开批先锁工单，再用已开批数量的合计校验余量。开批停在 Route 第一步。提交写入时，当前站点不在工单工艺路线上不写 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotApplicationService {

    private static final Set<String> TYPES = Set.of(
            "PRODUCTION", "ENGINEERING", "MONITOR", "DUMMY", "CONTROL", "TEST");

    private final LotRepository repository;
    private final WorkOrderRepository workOrderRepository;
    private final RouteStartProvider routeStartProvider;
    private final RouteNextProvider routeNext;
    private final LotStepPassRepository passes;
    private final LotHoldRepository holds;
    private final HoldReasonRepository holdReasons;
    private final TimeLinkProvider timeLinks;
    private final LotTimeLinkTimerRepository timers;
    private final WaferRepository wafers;
    private final Clock clock;

    /**
     * 从已下达的工单开一批。
     *
     * @param woId 工单
     * @param request Lot 号、类型和数量
     * @throws BusinessException 工单未下达、数量超出计划、Lot 号重复或类型不合法
     */
    @Transactional
    public void start(Long woId, LotStartRequest request) {
        checked(woId, request.getLotNo(), request.getQty());
        WorkOrder workOrder = workOrderRepository.findById(woId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在"));
        String type = request.getLotType() == null || request.getLotType().isBlank() ? "PRODUCTION" : request.getLotType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesExecErrorCodes.LOT_TYPE_INVALID, "Lot 类型不合法: " + type);
        }
        open(workOrder, woId, request.getLotNo(), type, request.getQty(), request.getRemark(),
                routeStartProvider.first(workOrder.getRouteVersionId()), null, 1, 0, null, null, null, null, null, null);
    }

    /**
     * 导入一条期初在制，停在指定站点。
     *
     * @param woId 工单
     * @param request 批号、数量和当前站点
     * @throws BusinessException 工单未下达、数量超出计划、批号重复、或站点不合法
     */
    @Transactional
    public void importWip(Long woId, WipImportRequest request) {
        if (request.getStepEnterAt() != null && request.getLastTrackOutAt() != null
                && request.getStepEnterAt().isBefore(request.getLastTrackOutAt())) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "进站时间不能早于上次出站时间");
        }
        if (request.getSeqNo() == null || request.getSeqNo() < 1 || request.getStepId() == null) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "期初导入必须指定站点");
        }
        int passCount = request.getPassCount() == null ? 0 : request.getPassCount();
        if (passCount < 0) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "经过次数不能小于 0");
        }
        boolean priorStep = request.getPriorStepId() != null;
        boolean priorCount = request.getPriorPassCount() != null;
        if (priorStep != priorCount || (priorCount && request.getPriorPassCount() < 1)
                || (priorStep && request.getPriorStepId().equals(request.getStepId()))) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "已经过站点要单独指定，次数至少 1");
        }
        boolean rework = request.getReworkId() != null;
        boolean reworkSeq = request.getReworkSeqNo() != null;
        if (rework != reworkSeq || (reworkSeq && request.getReworkSeqNo() < 1)) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "返工单和返工步骤序号要一起填，序号至少 1");
        }
        if (request.getLocationId() != null && request.getLocationId() < 1) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "库位不合法");
        }
        if (request.getRecipeVersionId() != null && request.getRecipeVersionId() < 1) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "Recipe 版本不合法");
        }
        boolean link = request.getTimeLinkId() != null;
        boolean linkStart = request.getTimeLinkStartAt() != null;
        if (link != linkStart) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "时间链和计时起点要一起填");
        }
        boolean wafer = request.getWaferCode() != null && !request.getWaferCode().isBlank();
        boolean slot = request.getSlotNo() != null;
        if (wafer != slot || (slot && request.getSlotNo() < 1)) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "Wafer 编号和槽位要一起填，槽位至少 1");
        }
        if (wafer && wafers.findByCode(request.getWaferCode()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.WAFER_CODE_EXISTS, "Wafer 编号已存在");
        }
        String waferStatus = request.getWaferStatus() == null || request.getWaferStatus().isBlank()
                ? "NORMAL" : request.getWaferStatus();
        if (wafer && !"NORMAL".equals(waferStatus) && !"SCRAPPED".equals(waferStatus)) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "Wafer 状态只能是 NORMAL 或 SCRAPPED");
        }
        if (wafer && "SCRAPPED".equals(waferStatus)
                && (request.getScrapReason() == null || request.getScrapReason().isBlank())) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "报废必须填写原因码");
        }
        HoldReason reason = holdReason(request.getHoldReasonCode());
        checked(woId, request.getLotNo(), request.getQty());
        WorkOrder workOrder = workOrderRepository.findById(woId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在"));
        boolean onRoute = routeNext.at(workOrder.getRouteVersionId(), request.getSeqNo())
                .filter(step -> request.getStepId().equals(step.stepId()))
                .isPresent();
        if (!onRoute) {
            throw new BusinessException(MesExecErrorCodes.WIP_STEP, "当前站点不在工单工艺路线上");
        }
        Lot lot = open(workOrder, woId, request.getLotNo(), "PRODUCTION", request.getQty(), null,
                new RouteStepStart(request.getSeqNo(), request.getStepId()), "IMPORT", passCount + 1,
                reason == null ? 0 : 1, request.getStepEnterAt(), request.getLastTrackOutAt(),
                request.getReworkId(), request.getReworkSeqNo(), request.getLocationId(),
                request.getRecipeVersionId());
        if (passCount > 0) {
            savePass(lot.getId(), request.getStepId(), passCount);
        }
        if (priorStep) {
            savePass(lot.getId(), request.getPriorStepId(), request.getPriorPassCount());
        }
        if (link) {
            TimeLinkStart timeLink = timeLinks.find(workOrder.getRouteVersionId(), request.getTimeLinkId())
                    .filter(row -> row.maxMinutes() != null && row.maxMinutes() >= 1 && row.toSeqNo() != null)
                    .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WIP_STEP, "时间链不在这个工艺上"));
            LotTimeLinkTimer timer = new LotTimeLinkTimer();
            timer.setLotId(lot.getId());
            timer.setTimeLinkId(timeLink.id());
            timer.setRouteVersionId(lot.getRouteVersionId());
            timer.setFromSeqNo(timeLink.fromSeqNo());
            timer.setToSeqNo(timeLink.toSeqNo());
            timer.setStartAt(request.getTimeLinkStartAt());
            timer.setDeadlineAt(request.getTimeLinkStartAt().plusMinutes(timeLink.maxMinutes()));
            timer.setStatus("ACTIVE");
            timer.setAlertLevel("NONE");
            timer.setAction(timeLink.action());
            timer.setReasonCode(timeLink.reasonCode());
            timers.save(timer);
        }
        if (wafer) {
            if (wafers.listByLot(lot.getId()).stream().anyMatch(row -> request.getSlotNo().equals(row.getSlotNo()))) {
                throw new BusinessException(MesExecErrorCodes.WIP_STEP, "槽位不能重复");
            }
            Wafer piece = new Wafer();
            piece.setWaferCode(request.getWaferCode());
            piece.setLotId(lot.getId());
            piece.setSlotNo(request.getSlotNo());
            piece.setStatus(waferStatus);
            piece.setPurpose("PRODUCT");
            piece.setDummyUseCount(0);
            piece.setVersion(0);
            if ("SCRAPPED".equals(waferStatus)) {
                piece.setScrapReason(request.getScrapReason());
                piece.setScrappedStepId(lot.getStepId());
                piece.setScrappedAt(LocalDateTime.now(clock));
            }
            wafers.save(piece);
            if ("NORMAL".equals(waferStatus)) {
                lot.setWaferCount(1);
                repository.update(lot);
            }
        }
        if (reason != null) {
            LotHold hold = new LotHold();
            hold.setLotId(lot.getId());
            hold.setReasonCode(reason.getReasonCode());
            hold.setHoldType(reason.getHoldType());
            hold.setHoldSource("IMPORT");
            hold.setStatus("ACTIVE");
            hold.setHoldStepId(lot.getStepId());
            hold.setHoldSeqNo(lot.getRouteSeqNo());
            hold.setHoldAt(LocalDateTime.now(clock));
            hold.setSourceRefId(0L);
            holds.save(hold);
        }
    }

    private HoldReason holdReason(String reasonCode) {
        if (reasonCode == null || reasonCode.isBlank()) {
            return null;
        }
        HoldReason reason = holdReasons.findByCode(reasonCode)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.HOLD_REASON_NOT_FOUND, "Hold 原因不存在"));
        if (reason.getEnabled() == null || reason.getEnabled() != 1) {
            throw new BusinessException(MesExecErrorCodes.LOT_HOLD_REASON_DISABLED, "Hold 原因已停用");
        }
        return reason;
    }

    private void savePass(Long lotId, Long stepId, int passCount) {
        LotStepPass pass = new LotStepPass();
        pass.setLotId(lotId);
        pass.setStepId(stepId);
        pass.setPassCount(passCount);
        passes.save(pass);
    }

    private void checked(Long woId, String lotNo, Long qty) {
        workOrderRepository.lock(woId);
        WorkOrder workOrder = workOrderRepository.findById(woId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在"));
        if (!"RELEASED".equals(workOrder.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_WO_NOT_RELEASED, "只有已下达的工单可以开批");
        }
        if (qty == null || qty <= 0) {
            throw new BusinessException(MesExecErrorCodes.LOT_QTY_INVALID, "开批数量必须大于 0");
        }
        if (repository.findByNo(lotNo).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.LOT_NO_EXISTS, "Lot 号已存在: " + lotNo);
        }
        long used = repository.sumInitialQty(woId);
        if (used + qty > workOrder.getPlanQty()) {
            throw new BusinessException(MesExecErrorCodes.LOT_OVER_PLAN, "开批数量超过工单计划量");
        }
    }

    private Lot open(WorkOrder workOrder, Long woId, String lotNo, String type, Long qty, String remark,
            RouteStepStart step, String source, int passNo, int holdCount, LocalDateTime stepEnterAt,
            LocalDateTime lastTrackOutAt, Long reworkId, Integer reworkSeqNo, Long locationId,
            Long recipeVersionId) {
        LocalDateTime now = LocalDateTime.now(clock);
        Lot lot = new Lot();
        lot.setLotNo(lotNo);
        lot.setLotType(type);
        lot.setWoId(woId);
        lot.setProductId(workOrder.getProductId());
        lot.setProductVersionId(workOrder.getProductVersionId());
        lot.setRouteVersionId(workOrder.getRouteVersionId());
        lot.setBomVersionId(workOrder.getBomVersionId());
        lot.setRouteSeqNo(step.seqNo());
        lot.setStepId(step.stepId());
        lot.setPassNo(passNo);
        lot.setStatus("WAITING");
        lot.setHoldCount(holdCount);
        lot.setQty(qty);
        lot.setQtyUnitId(workOrder.getQtyUnitId());
        lot.setInitialQty(qty);
        lot.setScrapQty(0L);
        lot.setCurrentScrapQty(0L);
        lot.setWaferCount(0);
        lot.setPriority(workOrder.getPriority());
        lot.setIsHot(0);
        lot.setStepEnterAt(stepEnterAt == null ? now : stepEnterAt);
        lot.setLastTrackOutAt(lastTrackOutAt);
        lot.setReworkId(reworkId);
        lot.setReworkSeqNo(reworkSeqNo);
        lot.setLocationId(locationId);
        lot.setRecipeVersionId(recipeVersionId);
        lot.setStartedAt(now);
        lot.setRemark(remark);
        lot.setSource(source);
        lot.setVersion(0);
        repository.save(lot);
        return lot;
    }
}
