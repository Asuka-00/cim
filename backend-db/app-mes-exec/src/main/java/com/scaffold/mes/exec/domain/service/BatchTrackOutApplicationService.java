package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.TrackOutRequest;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * BatchTrackOutApplicationService 批次出站
 * <p>
 * 只接受 RUNNING。同一事务内逐个 Lot 出站，任何一个失败都回滚。批次随后变为 COMPLETED，并清空成员的 batch_id。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class BatchTrackOutApplicationService {

    private final MesTransactionService transactions;
    private final BatchRepository batchRepository;
    private final BatchLotRepository memberRepository;
    private final LotRepository lotRepository;
    private final TrackOutApplicationService trackOut;
    private final Clock clock;

    /**
     * 批次出站。
     *
     * @param batchId 批次
     * @param request 备注，可空
     * @throws BusinessException 批次不是 RUNNING，或没有成员
     */
    public void trackOut(Long batchId, TrackOutRequest request) {
        Batch loaded = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_NOT_FOUND, "批次不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("BATCH_TRACK_OUT");
        command.setObjectType("BATCH");
        command.setObjectId(batchId);
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        if (loaded.getEquipmentId() != null) {
            locks.add(new MesLockTarget("EQUIPMENT", loaded.getEquipmentId()));
        }
        for (BatchLot member : memberRepository.listIn(batchId)) {
            locks.add(new MesLockTarget("LOT", member.getLotId()));
        }
        command.setLocks(locks);
        String remark = request == null ? null : request.getRemark();
        transactions.execute(command, () -> {
            apply(loaded.getId(), remark);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long batchId, String remark) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_NOT_FOUND, "批次不存在"));
        if (!"RUNNING".equals(batch.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "只有 RUNNING 的批次可以出站");
        }
        var members = memberRepository.listIn(batchId);
        if (members.isEmpty()) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "批次没有成员");
        }
        var ordered = members.stream()
                .sorted(Comparator.comparing(BatchLot::getSeq, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        boolean first = true;
        for (BatchLot member : ordered) {
            trackOut.trackOutFromBatch(member.getLotId(), remark, first);
            first = false;
        }
        batch.setStatus("COMPLETED");
        batch.setTrackOutAt(LocalDateTime.now(clock));
        batchRepository.update(batch);
        for (BatchLot member : members) {
            Lot lot = lotRepository.findById(member.getLotId())
                    .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
            lot.setBatchId(null);
            lotRepository.update(lot);
        }
    }
}
