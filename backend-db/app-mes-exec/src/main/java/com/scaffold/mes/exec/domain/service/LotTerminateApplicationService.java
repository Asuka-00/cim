package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotTerminateRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;

import lombok.RequiredArgsConstructor;

/**
 * LotTerminateApplicationService 终止
 * <p>
 * 在制 Lot 变为 TERMINATED。数量和报废数都不改，避免和报废混在一起。若正在设备上加工，离开后按出站规则回待机。不锁工单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotTerminateApplicationService {

    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final EquipmentTrackPort equipmentTrack;
    private final LotHistoryApplicationService history;
    private final Clock clock;

    /**
     * 终止一批。
     *
     * @param lotId Lot
     * @param request 原因码
     * @throws BusinessException 原因为空，或 Lot 已终态
     */
    public void terminate(Long lotId, LotTerminateRequest request) {
        if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_TERMINATE_REASON_REQUIRED, "终止必须填写原因");
        }
        Lot loaded = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("TERMINATE");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setReasonCode(request.getReasonCode());
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        if (loaded.getEquipmentId() != null) {
            locks.add(new MesLockTarget("EQUIPMENT", loaded.getEquipmentId()));
        }
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            apply(lotId, request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long lotId, LotTerminateRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_TERMINATE_CLOSED, "终态 Lot 不能终止");
        }
        String before = lot.getStatus();
        Long equipmentId = lot.getEquipmentId();
        long others = equipmentId == null ? 0 : Math.max(0, lotRepository.countRunning(equipmentId) - ("RUNNING".equals(before) ? 1 : 0));
        lot.setStatus("TERMINATED");
        lot.setFinishedAt(LocalDateTime.now(clock));
        lot.setEquipmentId(null);
        lot.setPortId(null);
        lot.setRecipeVersionId(null);
        lotRepository.update(lot);
        if (equipmentId != null) {
            equipmentTrack.releaseIfIdle(equipmentId, others);
        }
        history.record(lot, "TERMINATE", request.getReasonCode(), request.getRemark(), null, before, lot.getQty());
        if (lot.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", lot.getWoId(), null, "{\"wo_id\":" + lot.getWoId() + "}");
        }
    }
}
