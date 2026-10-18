package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotMergeRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotMergeApplicationService 合批
 * <p>
 * 两边都必须是未 Hold 的 WAITING，并且工单、产品版本、Route、当前步骤、计数单位和站点经过次数一致。源 Lot 的数量并入目标后清零，状态变为 MERGED。不改工单行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotMergeApplicationService {

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final WaferRepository waferRepository;
    private final LotStepPassRepository stepPassRepository;
    private final LotRelationRepository relationRepository;
    private final LotHistoryApplicationService lotHistory;
    private final WaferHistoryApplicationService waferHistory;
    private final GenealogyLinkApplicationService genealogy;
    private final Clock clock;

    /**
     * 把源 Lot 并入目标 Lot。
     *
     * @param targetLotId 目标 Lot
     * @param request 源 Lot
     * @throws BusinessException 条件不一致、有 Hold，或源和目标是同一批
     */
    public void merge(Long targetLotId, LotMergeRequest request) {
        if (targetLotId.equals(request.getSourceLotId())) {
            throw new BusinessException(MesExecErrorCodes.LOT_MERGE_REJECTED, "不能把 Lot 并入自己");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("MERGE");
        command.setObjectType("LOT");
        command.setObjectId(targetLotId);
        command.setSource("TERMINAL");
        command.setLocks(List.of(targetLotId, request.getSourceLotId()).stream()
                .sorted()
                .map(id -> new MesLockTarget("LOT", id))
                .toList());
        transactions.execute(command, () -> {
            apply(targetLotId, request.getSourceLotId());
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long targetId, Long sourceId) {
        Lot target = require(targetId);
        Lot source = require(sourceId);
        if (target.getBatchId() != null || source.getBatchId() != null) {
            throw new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "批次中的 Lot 不能合批");
        }
        if (!compatible(target, source) || !samePasses(targetId, sourceId)) {
            throw new BusinessException(MesExecErrorCodes.LOT_MERGE_REJECTED, "两个 Lot 不能合批");
        }
        List<Wafer> wafers = waferRepository.listByLot(sourceId);
        for (Wafer wafer : wafers) {
            waferRepository.lock(wafer.getId());
            wafer.setLotId(targetId);
            waferRepository.update(wafer);
            waferHistory.record(wafer, target, "MERGE", false, wafer.getSlotNo(), wafer.getStatus(), null, null, null);
        }
        target.setQty(nz(target.getQty()) + nz(source.getQty()));
        target.setInitialQty(nz(target.getInitialQty()) + nz(source.getInitialQty()));
        target.setScrapQty(nz(target.getScrapQty()) + nz(source.getScrapQty()));
        target.setCurrentScrapQty(nz(target.getCurrentScrapQty()) + nz(source.getCurrentScrapQty()));
        target.setWaferCount(nzInt(target.getWaferCount()) + nzInt(source.getWaferCount()));
        lotRepository.update(target);
        source.setQty(0L);
        source.setInitialQty(0L);
        source.setScrapQty(0L);
        source.setCurrentScrapQty(0L);
        source.setWaferCount(0);
        source.setStatus("MERGED");
        source.setFinishedAt(LocalDateTime.now(clock));
        lotRepository.update(source);
        LotRelation relation = new LotRelation();
        relation.setParentLotId(sourceId);
        relation.setChildLotId(targetId);
        relation.setRelationType("MERGE");
        relation.setQty(target.getQty());
        relation.setUnitId(target.getQtyUnitId());
        relation.setOccurredAt(LocalDateTime.now(clock));
        relationRepository.save(relation);
        genealogy.link("LOT", sourceId, source.getLotNo(), "LOT", targetId, target.getLotNo(),
                "MERGE", target.getStepId(), BigDecimal.valueOf(wafers.size()), null);
        lotHistory.record(source, "MERGE", null, null, null, "WAITING", 0L);
        if (target.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", target.getWoId(), null,
                    "{\"wo_id\":" + target.getWoId() + "}");
        }
    }

    private boolean compatible(Lot target, Lot source) {
        return "WAITING".equals(target.getStatus())
                && "WAITING".equals(source.getStatus())
                && holdFree(target)
                && holdFree(source)
                && Objects.equals(target.getWoId(), source.getWoId())
                && Objects.equals(target.getProductVersionId(), source.getProductVersionId())
                && Objects.equals(target.getRouteVersionId(), source.getRouteVersionId())
                && Objects.equals(target.getStepId(), source.getStepId())
                && Objects.equals(target.getRouteSeqNo(), source.getRouteSeqNo())
                && Objects.equals(target.getQtyUnitId(), source.getQtyUnitId());
    }

    private boolean samePasses(Long targetId, Long sourceId) {
        return counts(targetId).equals(counts(sourceId));
    }

    private Map<Long, Integer> counts(Long lotId) {
        return stepPassRepository.listByLot(lotId).stream()
                .sorted(Comparator.comparing(LotStepPass::getStepId))
                .collect(Collectors.toMap(LotStepPass::getStepId, LotStepPass::getPassCount, (left, right) -> left));
    }

    private Lot require(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
    }

    private static boolean holdFree(Lot lot) {
        return lot.getHoldCount() == null || lot.getHoldCount() == 0;
    }

    private static long nz(Long value) {
        return value == null ? 0 : value;
    }

    private static int nzInt(Integer value) {
        return value == null ? 0 : value;
    }
}
