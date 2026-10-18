package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotCancelTrackInRequest;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * BatchEjectApplicationService 加工中剔除
 * <p>
 * 只接受 RUNNING。同一事务里取消该 Lot 的进站，标成 REMOVED，并清空 batch_id。批内没有成员时批次变为 CANCELLED。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class BatchEjectApplicationService {

    private final MesTransactionService transactions;
    private final BatchRepository batchRepository;
    private final BatchLotRepository memberRepository;
    private final LotRepository lotRepository;
    private final LotCancelTrackInApplicationService cancelTrackIn;
    private final Clock clock;

    /**
     * 从加工中的批次剔除一个 Lot。
     *
     * @param batchId 批次
     * @param lotId Lot
     * @param request 取消进站的原因
     * @throws BusinessException 批次不是 RUNNING、Lot 不在批内，或原因为空
     */
    public void eject(Long batchId, Long lotId, LotCancelTrackInRequest request) {
        if (request == null || request.getReasonCode() == null || request.getReasonCode().isBlank()) {
            throw new BusinessException(MesExecErrorCodes.LOT_CANCEL_TRACK_IN_REASON, "剔除必须填写原因");
        }
        Batch loaded = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_NOT_FOUND, "批次不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("BATCH_EJECT");
        command.setObjectType("BATCH");
        command.setObjectId(batchId);
        command.setReasonCode(request.getReasonCode());
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        if (loaded.getEquipmentId() != null) {
            locks.add(new MesLockTarget("EQUIPMENT", loaded.getEquipmentId()));
        }
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            apply(loaded.getId(), lotId, request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long batchId, Long lotId, LotCancelTrackInRequest request) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_NOT_FOUND, "批次不存在"));
        if (!"RUNNING".equals(batch.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "只有加工中的批次可以剔除");
        }
        BatchLot member = memberRepository.find(batchId, lotId)
                .filter(row -> "IN".equals(row.getStatus()))
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "Lot 不在该批次里"));
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        cancelTrackIn.cancelFromBatch(lotId, request);
        member.setStatus("REMOVED");
        member.setRemovedAt(LocalDateTime.now(clock));
        member.setRemoveReason(request.getReasonCode());
        memberRepository.update(member);
        lot.setBatchId(null);
        lotRepository.update(lot);
        int wafers = lot.getWaferCount() == null ? 0 : lot.getWaferCount();
        long qty = lot.getQty() == null ? 0 : lot.getQty();
        batch.setLotCount(batch.getLotCount() - 1);
        batch.setWaferCount(Math.max(0, batch.getWaferCount() - wafers));
        batch.setTotalQty(Math.max(0, batch.getTotalQty() - qty));
        if (batch.getLotCount() == 0) {
            batch.setStatus("CANCELLED");
        }
        batchRepository.update(batch);
    }
}
