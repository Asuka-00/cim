package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.TrackInRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.service.interlock.InterlockEngine;
import com.scaffold.mes.exec.domain.service.interlock.TrackInContext;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentGroupPort;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.mes.base.provider.QueueTimeProvider;
import com.scaffold.share.integration.mes.base.provider.StepToolPort;
import com.scaffold.share.integration.mes.base.provider.ToolLifePort;
import com.scaffold.share.integration.mes.base.provider.TrackResourceProvider;
import com.scaffold.share.integration.mes.base.provider.UserQualPort;
import com.scaffold.share.integration.mes.base.provider.UserScopePort;
import com.scaffold.share.integration.mes.base.response.QueueTimeLimit;
import com.scaffold.share.integration.mes.base.response.TrackResourceFacts;

import lombok.RequiredArgsConstructor;

/**
 * TrackInApplicationService 进站
 * <p>
 * 先按设备、Lot 的顺序加锁，再跑 Interlock。通过后 Lot 变为 RUNNING，写历史和 Lot 到设备的追溯。不锁工单，不逐片写 Wafer 历史。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class TrackInApplicationService {

    private static final Set<String> ENGINEERING_LOTS = Set.of("ENGINEERING", "MONITOR", "TEST");

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final TrackResourceProvider resources;
    private final InterlockEngine interlock;
    private final EquipmentTrackPort equipmentTrack;
    private final LotHistoryApplicationService history;
    private final GenealogyLinkApplicationService genealogy;
    private final CurrentSessionProvider session;
    private final LotTimeLinkTimerApplicationService timers;
    private final UserQualPort qualifications;
    private final UserScopePort scopes;
    private final QueueTimeProvider queueTimes;
    private final LotHoldApplicationService holds;
    private final ToolLifePort toolLife;
    private final StepToolPort stepTools;
    private final EquipmentGroupPort equipmentGroups;
    private final Clock clock;

    /**
     * 进站。
     *
     * @param lotId Lot
     * @param request 站点、设备和 Recipe
     * @throws BusinessException Lot 不存在或 Interlock 阻断
     */
    public void trackIn(Long lotId, TrackInRequest request) {
        Lot loaded = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (loaded.getBatchId() != null) {
            throw new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "批次中的 Lot 不能单独进站");
        }
        rejectQueue(loaded);
        check(loaded, request);
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("TRACK_IN");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        if (request.getEquipmentId() != null) {
            locks.add(new MesLockTarget("EQUIPMENT", request.getEquipmentId()));
        }
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            apply(lotId, request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    /**
     * 在批次事务里进站。调用方已经持有事务，不再单独开事务。
     *
     * @param lotId Lot
     * @param request 站点、设备和 Recipe
     */
    public void trackInFromBatch(Long lotId, TrackInRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        rejectQueue(lot);
        check(lot, request);
        apply(lotId, request);
    }

    private void check(Lot lot, TrackInRequest request) {
        TrackResourceFacts facts = resources.facts(request.getStepId(), lot.getRouteVersionId(), lot.getRouteSeqNo(),
                lot.getProductId(), request.getEquipmentId(), request.getPortId(), request.getRecipeVersionId());
        interlock.requirePass(new TrackInContext(lot, request.getStepId(), request.getEquipmentId(), request.getPortId(),
                request.getRecipeVersionId(), facts));
    }

    private void rejectQueue(Lot lot) {
        QueueTimeLimit limit = queueTimes.limit(lot.getRouteVersionId(), lot.getRouteSeqNo());
        if (limit.maxMinutes() == null || lot.getStepEnterAt() == null) {
            return;
        }
        if (!lot.getStepEnterAt().plusMinutes(limit.maxMinutes()).isBefore(LocalDateTime.now(clock))) {
            return;
        }
        if ("WARN".equals(limit.action())) {
            return;
        }
        holds.holdSystem(lot.getId(), "QUEUE_TIME", "QUEUE_TIME", lot.getStepId());
        throw new BusinessException(MesExecErrorCodes.QUEUE_TIME_EXCEEDED, "排队时间已超过上限");
    }

    private void apply(Long lotId, TrackInRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        TrackResourceFacts facts = resources.facts(request.getStepId(), lot.getRouteVersionId(), lot.getRouteSeqNo(),
                lot.getProductId(), request.getEquipmentId(), request.getPortId(), request.getRecipeVersionId());
        timers.close(lot);
        LocalDateTime now = LocalDateTime.now(clock);
        String user = session.getCurrentUserNameOrDefault();
        if (!scopes.allowed(user, "STEP", request.getStepId())) {
            throw new BusinessException(MesExecErrorCodes.STEP_OUT_OF_SCOPE, "当前用户不能操作这个站点");
        }
        if (!qualifications.allowed(user, "STEP", request.getStepId(), now)) {
            throw new BusinessException(MesExecErrorCodes.STEP_NOT_QUALIFIED, "当前用户不能操作这个站点");
        }
        if (request.getEquipmentId() != null && !qualifications.allowed(user,
                "EQUIPMENT", request.getEquipmentId(), now)) {
            throw new BusinessException(MesExecErrorCodes.PERSON_NOT_QUALIFIED, "当前用户不能操作这台设备");
        }
        Long groupId = request.getEquipmentId() == null ? null : equipmentGroups.groupId(request.getEquipmentId());
        if (groupId != null && !qualifications.allowed(user, "EQUIPMENT_GROUP", groupId, now)) {
            throw new BusinessException(MesExecErrorCodes.GROUP_NOT_QUALIFIED, "当前用户不能操作这个设备组");
        }
        if (request.getEquipmentId() != null
                && !scopes.allowed(user, "EQUIPMENT_GROUP", equipmentGroups.groupId(request.getEquipmentId()))) {
            throw new BusinessException(MesExecErrorCodes.EQUIPMENT_GROUP_OUT_OF_SCOPE, "当前用户不能操作这个设备组");
        }
        if (!stepTools.accepted(request.getStepId(), lot.getProductId(), request.getToolId(),
                lot.getRouteVersionId(), lot.getRouteSeqNo())) {
            throw new BusinessException(MesExecErrorCodes.TOOL_TYPE_MISMATCH, "工具类型不符合站点要求");
        }
        if (request.getToolId() != null && !toolLife.withinLife(request.getToolId())) {
            throw new BusinessException(MesExecErrorCodes.TOOL_LIFE_EXCEEDED, "工具已达到寿命上限");
        }
        String before = "{\"status\":\"" + lot.getStatus() + "\",\"stepId\":" + lot.getStepId() + "}";
        linkEquipment(lot, request.getEquipmentId(), facts);
        lot.setStatus("RUNNING");
        lot.setEquipmentId(request.getEquipmentId());
        lot.setPortId(request.getPortId());
        lot.setRecipeVersionId(request.getRecipeVersionId());
        lot.setTrackInAt(now);
        lot.setTrackInBy(session.getCurrentUserNameOrDefault());
        lot.setLocationType(request.getEquipmentId() == null ? null : "EQUIPMENT");
        lotRepository.update(lot);
        history.record(lot, "TRACK_IN", null, null, before, "WAITING", lot.getQty());
        if (request.getEquipmentId() != null) {
            genealogy.link("LOT", lot.getId(), lot.getLotNo(), "EQUIPMENT", request.getEquipmentId(), null,
                    "PROCESS", lot.getStepId(), lot.getQty() == null ? null : BigDecimal.valueOf(lot.getQty()), null);
        }
    }

    private void linkEquipment(Lot lot, Long equipmentId, TrackResourceFacts facts) {
        if (!facts.needEquipment() || equipmentId == null) {
            return;
        }
        boolean trackOwned = "STANDBY".equals(facts.equipmentState()) || "TRACK".equals(facts.statusSource());
        if (!trackOwned) {
            return;
        }
        String next = "STANDBY".equals(facts.equipmentState()) && ENGINEERING_LOTS.contains(lot.getLotType())
                ? "ENGINEERING"
                : "PRODUCTIVE";
        equipmentTrack.onTrackIn(equipmentId, next);
    }
}
