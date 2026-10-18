package com.scaffold.mes.exec.domain.service;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotCancelTrackInRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;

import lombok.RequiredArgsConstructor;

/**
 * LotCancelTrackInApplicationService 取消进站
 * <p>
 * 加工中的 Lot 回到 WAITING。清空设备、Port、Recipe 和进站时间。设备上没有其他在制批时，来源为 TRACK 的设备回到待机。不回减经过次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotCancelTrackInApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final EquipmentTrackPort equipmentTrack;
    private final LotHistoryApplicationService history;

    /**
     * 取消进站。
     *
     * @param lotId Lot
     * @param request 原因码
     * @throws BusinessException 不是加工中，或原因为空
     */
    public void cancel(Long lotId, LotCancelTrackInRequest request) {
        if (request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_CANCEL_TRACK_IN_REASON, "取消进站必须填写原因");
        }
        Lot loaded = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (loaded.getBatchId() != null) {
            throw new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "批次中的 Lot 不能单独取消进站");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("CANCEL_TRACK_IN");
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

    /**
     * 在批次剔除事务里取消进站。调用方已经持有事务，不再单独开事务。
     *
     * @param lotId Lot
     * @param request 原因码
     */
    public void cancelFromBatch(Long lotId, LotCancelTrackInRequest request) {
        apply(lotId, request);
    }

    private void apply(Long lotId, LotCancelTrackInRequest request) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!"RUNNING".equals(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_CANCEL_TRACK_IN_STATUS, "只有加工中的 Lot 可以取消进站");
        }
        Long equipmentId = lot.getEquipmentId();
        long others = equipmentId == null ? 0 : Math.max(0, lotRepository.countRunning(equipmentId) - 1);
        String before = "{\"status\":\"RUNNING\",\"equipmentId\":" + equipmentId + "}";
        lot.setStatus("WAITING");
        lot.setEquipmentId(null);
        lot.setPortId(null);
        lot.setRecipeVersionId(null);
        lot.setTrackInAt(null);
        lot.setTrackInBy(null);
        lot.setLocationType(null);
        lot.setLocationId(null);
        lotRepository.update(lot);
        if (equipmentId != null) {
            equipmentTrack.releaseIfIdle(equipmentId, others);
        }
        history.record(lot, "CANCEL_TRACK_IN", request.getReasonCode(), request.getRemark(), before, "RUNNING", lot.getQty());
    }
}
