package com.scaffold.mes.exec.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotUndoTrackOutRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotSamplingPlanRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.mes.base.provider.ToolLifePort;
import com.scaffold.share.integration.mes.base.provider.TrackResourceProvider;

import lombok.RequiredArgsConstructor;

/**
 * LotUndoTrackOutApplicationService 撤销出站
 * <p>
 * 用最近一次出站保存的快照回到 RUNNING，并回减那一次经过。删掉这次出站写下的抽检决定，计数不回退。原设备已经满员时拒绝。不锁工单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotUndoTrackOutApplicationService {

    private static final Set<String> CAN_UNDO = Set.of("WAITING", "COMPLETED");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final LotHistoryRepository historyRepository;
    private final LotStepPassApplicationService stepPass;
    private final TrackResourceProvider resources;
    private final EquipmentTrackPort equipmentTrack;
    private final LotHistoryApplicationService history;
    private final ToolLifePort toolLife;
    private final MaterialConsumptionApplicationService consumptions;
    private final LotSamplingPlanRepository samplingPlans;

    /**
     * 撤销最近一次出站。
     *
     * @param lotId Lot
     * @param request 原因码
     * @throws BusinessException 没有出站可撤、状态不对，或原设备已满
     */
    public void undo(Long lotId, LotUndoTrackOutRequest request) {
        if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_UNDO_TRACK_OUT_MISSING, "撤销出站必须填写原因");
        }
        Lot loaded = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("UNDO_TRACK_OUT");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setReasonCode(request.getReasonCode());
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            apply(loaded.getId(), request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long lotId, LotUndoTrackOutRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!CAN_UNDO.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_UNDO_TRACK_OUT_STATUS, "当前状态不能撤销出站");
        }
        LotHistory trackOut = historyRepository.findLatest(lotId, "TRACK_OUT")
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_UNDO_TRACK_OUT_MISSING, "没有可撤销的出站"));
        JsonNode before = read(trackOut.getBeforeJson());
        Long equipmentId = longOrNull(before, "equipmentId");
        if (equipmentId != null) {
            int capacity = resources.facts(longOrNull(before, "stepId"), lot.getRouteVersionId(), lot.getRouteSeqNo(),
                    lot.getProductId(), equipmentId, longOrNull(before, "portId"), longOrNull(before, "recipeVersionId"))
                    .equipmentCapacity();
            if (lotRepository.countRunning(equipmentId) >= capacity) {
                throw new BusinessException(MesExecErrorCodes.LOT_UNDO_TRACK_OUT_CAPACITY, "原设备已经没有空位");
            }
        }
        if (request.getToolId() != null) {
            toolLife.reverse(request.getToolId());
        }
        Long leftStep = longOrNull(before, "stepId");
        Integer leftSeq = before.path("seq").isMissingNode() || before.path("seq").isNull() ? null : before.path("seq").asInt();
        if (leftSeq != null) {
            samplingPlans.deleteAfter(lotId, leftSeq);
        }
        consumptions.reverse(lot, leftStep);
        stepPass.undoTrackOut(lotId, leftStep);
        lot.setStatus("RUNNING");
        lot.setStepId(leftStep);
        lot.setRouteSeqNo(leftSeq);
        lot.setEquipmentId(equipmentId);
        lot.setPortId(longOrNull(before, "portId"));
        lot.setRecipeVersionId(longOrNull(before, "recipeVersionId"));
        if (before.hasNonNull("stepEnterAt")) {
            lot.setStepEnterAt(LocalDateTime.parse(before.get("stepEnterAt").asText()));
        }
        lot.setFinishedAt(null);
        lot.setLocationType(equipmentId == null ? null : "EQUIPMENT");
        lot.setLocationId(equipmentId);
        lotRepository.update(lot);
        if (equipmentId != null) {
            equipmentTrack.onTrackIn(equipmentId, "PRODUCTIVE");
        }
        history.record(lot, "UNDO_TRACK_OUT", request.getReasonCode(), request.getRemark(), null, "WAITING", lot.getQty());
    }

    private static JsonNode read(String json) {
        try {
            if (json == null || json.isBlank()) {
                throw new BusinessException(MesExecErrorCodes.LOT_UNDO_TRACK_OUT_MISSING, "出站没有变更前快照");
            }
            return JSON.readTree(json);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(MesExecErrorCodes.LOT_UNDO_TRACK_OUT_MISSING, "出站快照无法读取");
        }
    }

    private static Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asLong();
    }
}
