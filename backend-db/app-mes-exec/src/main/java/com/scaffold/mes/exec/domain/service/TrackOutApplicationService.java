package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.TrackOutRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.BomConsumePort;
import com.scaffold.share.integration.mes.base.provider.CollectSpecPort;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.mes.base.provider.ToolLifePort;
import com.scaffold.share.integration.mes.base.response.BomConsumeLine;
import com.scaffold.share.integration.mes.warehouse.provider.EquipmentBindingPort;
import com.scaffold.share.integration.mes.warehouse.provider.LinesideStockPort;
import com.scaffold.share.integration.mes.warehouse.response.BindingDeduction;
import com.scaffold.share.integration.mes.warehouse.response.StockDeduction;
import com.scaffold.share.integration.mes.base.provider.ReworkRouteProvider;
import com.scaffold.share.integration.mes.base.provider.RouteNextProvider;
import com.scaffold.share.integration.mes.base.provider.SamplingRulePort;
import com.scaffold.share.integration.mes.base.response.ReworkProgress;
import com.scaffold.share.integration.mes.base.response.RouteStepNext;
import com.scaffold.share.integration.mes.base.response.SamplingBinding;

import lombok.RequiredArgsConstructor;

/**
 * TrackOutApplicationService 出站
 * <p>
 * 只接受 RUNNING。经过次数加 1，然后停到下一个序号；没有下一步则完成。清空设备。Hold 不阻止出站。不锁工单，不逐片写 Wafer 历史。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class TrackOutApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final LotStepPassApplicationService stepPass;
    private final RouteNextProvider routeNext;
    private final ReworkRouteProvider reworkRoute;
    private final EquipmentTrackPort equipmentTrack;
    private final LotHistoryApplicationService history;
    private final FutureActionApplicationService futureActions;
    private final UnitConversionApplicationService conversions;
    private final LotTimeLinkTimerApplicationService timers;
    private final ToolLifePort toolLife;
    private final BomConsumePort bomConsume;
    private final LinesideStockPort stock;
    private final EquipmentBindingPort bindings;
    private final MaterialConsumptionApplicationService consumptions;
    private final SamplingRulePort samplingRules;
    private final SamplingDecisionApplicationService samplingDecisions;
    private final CollectSpecPort collectSpecs;
    private final CollectResultApplicationService collects;
    private final Clock clock;

    /**
     * 出站。
     *
     * @param lotId Lot
     * @param request 备注，可空
     * @throws BusinessException Lot 不存在或不在加工中
     */
    public void trackOut(Long lotId, TrackOutRequest request) {
        Lot loaded = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (loaded.getBatchId() != null) {
            throw new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "批次中的 Lot 不能单独出站");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("TRACK_OUT");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setObjectCode(loaded.getLotNo());
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        if (loaded.getEquipmentId() != null) {
            locks.add(new MesLockTarget("EQUIPMENT", loaded.getEquipmentId()));
        }
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            apply(lotId, request, true);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    /**
     * 在批次事务里出站。调用方已经持有事务，不再单独开事务。
     *
     * @param lotId Lot
     * @param remark 备注，可空
     * @param perRun 为 true 时扣 PER_RUN 和 PER_TIME。批次里只有序号最小的成员为 true
     */
    public void trackOutFromBatch(Long lotId, String remark, boolean perRun) {
        TrackOutRequest request = new TrackOutRequest();
        request.setRemark(remark);
        apply(lotId, request, perRun);
    }

    private void consume(Lot lot, BomConsumeLine line, BigDecimal qty, String mode) {
        if (lot.getEquipmentId() != null) {
            BindingDeduction bound = bindings.deduct(lot.getEquipmentId(), line.materialId(), qty);
            if (bound != null) {
                consumptions.recordBound(lot, line.materialId(), qty, mode, bound);
                return;
            }
        }
        StockDeduction taken = stock.deduct(line.materialId(), qty);
        if (taken == null) {
            consumptions.record(lot, line.materialId(), qty, mode, new StockDeduction(0L, 0L, 0L), true);
            return;
        }
        consumptions.record(lot, line.materialId(), qty, mode, taken, false);
    }

    private long processMinutes(Lot lot) {
        if (lot.getTrackInAt() == null) {
            return 0;
        }
        return Math.max(0, Duration.between(lot.getTrackInAt(), LocalDateTime.now(clock)).toMinutes());
    }

    private static java.util.List<BomConsumeLine> empty(java.util.List<BomConsumeLine> lines) {
        return lines == null ? java.util.List.of() : lines;
    }

    private void apply(Long lotId, TrackOutRequest request, boolean perRun) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"RUNNING".equals(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_NOT_RUNNING, "只有加工中的 Lot 可以出站");
        }
        List<Long> required = collectSpecs.requiredItems(lot.getRouteVersionId(), lot.getRouteSeqNo(), lot.getStepId(),
                lot.getProductId());
        if (required != null) {
            for (Long itemId : required) {
                if (collects.effective(lot.getId(), itemId, lot.getRouteSeqNo(), lot.getPassNo()).isEmpty()) {
                    throw new BusinessException(MesExecErrorCodes.COLLECT_REQUIRED, "必采项还没有有效值");
                }
            }
        }
        if (request != null && request.getToolId() != null) {
            toolLife.use(request.getToolId());
        }
        if (lot.getBomVersionId() != null) {
            for (BomConsumeLine line : bomConsume.perLot(lot.getBomVersionId(), lot.getStepId())) {
                consume(lot, line, line.qtyPer(), "PER_LOT");
            }
            int wafers = lot.getWaferCount() == null ? 0 : lot.getWaferCount();
            if (wafers > 0) {
                BigDecimal count = BigDecimal.valueOf(wafers);
                for (BomConsumeLine line : empty(bomConsume.perWafer(lot.getBomVersionId(), lot.getStepId()))) {
                    consume(lot, line, line.qtyPer().multiply(count), "PER_WAFER");
                }
            }
            long dies = lot.getQty() == null ? 0L : lot.getQty();
            if (dies > 0) {
                BigDecimal count = BigDecimal.valueOf(dies);
                for (BomConsumeLine line : empty(bomConsume.perDie(lot.getBomVersionId(), lot.getStepId()))) {
                    consume(lot, line, line.qtyPer().multiply(count), "PER_DIE");
                }
            }
            long strips = lot.getSubQty() == null ? 0L : lot.getSubQty();
            if (strips > 0) {
                BigDecimal count = BigDecimal.valueOf(strips);
                for (BomConsumeLine line : empty(bomConsume.perStrip(lot.getBomVersionId(), lot.getStepId()))) {
                    consume(lot, line, line.qtyPer().multiply(count), "PER_STRIP");
                }
            }
            if (perRun) {
                long minutes = processMinutes(lot);
                if (minutes > 0) {
                    BigDecimal count = BigDecimal.valueOf(minutes);
                    for (BomConsumeLine line : empty(bomConsume.perTime(lot.getBomVersionId(), lot.getStepId()))) {
                        consume(lot, line, line.qtyPer().multiply(count), "PER_TIME");
                    }
                }
                for (BomConsumeLine line : empty(bomConsume.perRun(lot.getBomVersionId(), lot.getStepId()))) {
                    consume(lot, line, line.qtyPer(), "PER_RUN");
                }
            }
        }
        String before = "{\"status\":\"RUNNING\",\"stepId\":" + lot.getStepId()
                + ",\"seq\":" + lot.getRouteSeqNo()
                + ",\"equipmentId\":" + lot.getEquipmentId()
                + ",\"portId\":" + lot.getPortId()
                + ",\"recipeVersionId\":" + lot.getRecipeVersionId()
                + ",\"stepEnterAt\":" + (lot.getStepEnterAt() == null ? "null" : "\"" + lot.getStepEnterAt() + "\"")
                + "}";
        Long equipmentId = lot.getEquipmentId();
        long others = equipmentId == null ? 0 : Math.max(0, lotRepository.countRunning(equipmentId) - 1);
        long qtyBefore = lot.getQty() == null ? 0L : lot.getQty();
        String remark = request == null ? null : request.getRemark();
        conversions.apply(lot, request);
        stepPass.recordTrackOut(lot.getId(), lot.getStepId());
        LocalDateTime now = LocalDateTime.now(clock);
        if (lot.getReworkId() == null) {
            timers.open(lot, now);
        }
        if (lot.getReworkId() != null) {
            moveRework(lot, now);
        } else {
            moveMain(lot, now);
        }
        lot.setEquipmentId(null);
        lot.setPortId(null);
        lot.setRecipeVersionId(null);
        lot.setTrackInAt(null);
        lot.setTrackInBy(null);
        lot.setLastTrackOutAt(now);
        if (equipmentId != null) {
            equipmentTrack.releaseIfIdle(equipmentId, others);
        }
        lotRepository.update(lot);
        history.record(lot, "TRACK_OUT", null, remark, before, "RUNNING", qtyBefore);
        String payload = "{\"lot_id\":" + lot.getId() + ",\"qty\":" + lot.getQty() + ",\"step_id\":" + lot.getStepId() + "}";
        transactions.publish("LOT_TRACK_OUT", "LOT", lot.getId(), null, payload);
        if (lot.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", lot.getWoId(), null, "{\"wo_id\":" + lot.getWoId() + "}");
        }
    }

    private void moveMain(Lot lot, LocalDateTime now) {
        RouteStepNext next = skipUnsampled(lot, lot.getRouteSeqNo());
        if (next == null) {
            lot.setStatus("COMPLETED");
            lot.setFinishedAt(now);
            lot.setLocationType(null);
            lot.setLocationId(null);
            return;
        }
        arrive(lot, next.seqNo(), next.stepId(), next.areaId(), now);
    }

    private void moveRework(Lot lot, LocalDateTime now) {
        ReworkProgress progress = reworkRoute.nextAfter(lot.getReworkId(), lot.getReworkSeqNo())
                .filter(row -> row.stepId() != null)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_REWORK_NOWHERE, "返工没有可去的下一步"));
        if (progress.backToMain()) {
            lot.setReworkId(null);
            lot.setReworkSeqNo(null);
            arrive(lot, progress.routeSeqNo(), progress.stepId(), progress.areaId(), now);
            return;
        }
        lot.setReworkSeqNo(progress.reworkSeqNo());
        lot.setStatus("WAITING");
        lot.setStepId(progress.stepId());
        lot.setPassNo(stepPass.arrivalPassNo(lot.getId(), progress.stepId()));
        lot.setStepEnterAt(now);
        lot.setLocationType("AREA");
        lot.setLocationId(progress.areaId());
    }

    private RouteStepNext skipUnsampled(Lot lot, Integer fromSeq) {
        RouteStepNext next = routeNext.next(lot.getRouteVersionId(), fromSeq).orElse(null);
        while (next != null && routeNext.sampling(lot.getRouteVersionId(), next.seqNo())) {
            SamplingBinding rule = samplingRules.binding(lot.getRouteVersionId(), next.seqNo(), next.stepId(),
                    lot.getProductId()).orElse(null);
            if (rule == null) {
                return next;
            }
            int passNo = stepPass.arrivalPassNo(lot.getId(), next.stepId());
            boolean need = "PERCENT".equals(rule.sampleType())
                    ? samplingDecisions.decideByPercent(lot.getId(), next.seqNo(), passNo, next.stepId(), rule.ruleId(),
                            0L, lot.getProductId(), rule.percent(), rule.waferCount(), rule.slotList())
                    : samplingDecisions.decide(lot.getId(), next.seqNo(), passNo, next.stepId(), rule.ruleId(),
                            0L, lot.getProductId(), rule.nValue(), rule.waferCount(), rule.slotList());
            if (need) {
                return next;
            }
            next = routeNext.next(lot.getRouteVersionId(), next.seqNo()).orElse(null);
        }
        return next;
    }

    private void arrive(Lot lot, Integer seqNo, Long stepId, Long areaId, LocalDateTime now) {
        lot.setStatus("WAITING");
        lot.setRouteSeqNo(seqNo);
        lot.setStepId(stepId);
        lot.setPassNo(stepPass.arrivalPassNo(lot.getId(), stepId));
        lot.setStepEnterAt(now);
        lot.setLocationType("AREA");
        lot.setLocationId(areaId);
        futureActions.onArrive(lot);
    }
}
