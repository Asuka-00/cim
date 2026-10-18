package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.BatchTrackInRequest;
import com.scaffold.mes.exec.domain.dto.TrackInRequest;
import com.scaffold.mes.exec.domain.entity.Batch;
import com.scaffold.mes.exec.domain.entity.BatchLot;
import com.scaffold.mes.exec.domain.repository.BatchLotRepository;
import com.scaffold.mes.exec.domain.repository.BatchRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * BatchTrackInApplicationService 批次进站
 * <p>
 * 只接受 READY。同一事务内逐个 Lot 进站，任何一个失败都回滚。批次随后变为 RUNNING。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class BatchTrackInApplicationService {

    private final MesTransactionService transactions;
    private final BatchRepository batchRepository;
    private final BatchLotRepository memberRepository;
    private final TrackInApplicationService trackIn;
    private final Clock clock;

    /**
     * 批次进站。
     *
     * @param batchId 批次
     * @param request 设备和 Recipe
     * @throws BusinessException 批次不是 READY，或没有成员
     */
    public void trackIn(Long batchId, BatchTrackInRequest request) {
        Batch loaded = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_NOT_FOUND, "批次不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("BATCH_TRACK_IN");
        command.setObjectType("BATCH");
        command.setObjectId(batchId);
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        locks.add(new MesLockTarget("EQUIPMENT", request.getEquipmentId()));
        for (BatchLot member : memberRepository.listIn(batchId)) {
            locks.add(new MesLockTarget("LOT", member.getLotId()));
        }
        command.setLocks(locks);
        transactions.execute(command, () -> {
            apply(loaded.getId(), request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long batchId, BatchTrackInRequest request) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.BATCH_NOT_FOUND, "批次不存在"));
        if (!"READY".equals(batch.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "只有 READY 的批次可以进站");
        }
        var members = memberRepository.listIn(batchId);
        int minLots = batch.getMinLots() == null ? 1 : batch.getMinLots();
        if (members.size() < minLots) {
            throw new BusinessException(MesExecErrorCodes.BATCH_CLOSED, "未达到最小批量");
        }
        TrackInRequest track = new TrackInRequest();
        track.setStepId(batch.getStepId());
        track.setEquipmentId(request.getEquipmentId());
        track.setRecipeVersionId(request.getRecipeVersionId());
        for (BatchLot member : members) {
            trackIn.trackInFromBatch(member.getLotId(), track);
        }
        batch.setStatus("RUNNING");
        batch.setEquipmentId(request.getEquipmentId());
        batch.setTrackInAt(LocalDateTime.now(clock));
        batchRepository.update(batch);
    }
}
