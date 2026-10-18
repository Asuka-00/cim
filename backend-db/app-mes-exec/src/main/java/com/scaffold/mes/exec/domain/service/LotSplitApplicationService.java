package com.scaffold.mes.exec.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotSplitRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.entity.LotStepPass;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotHoldRepository;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.LotStepPassRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotSplitApplicationService 拆批
 * <p>
 * 子 Lot 号是父 Lot 号加点号和拆批序号。子 Lot 沿用步骤、经过次数、进站时间和未释放的 Hold。位置不继承。父 Lot 至少留下一片。不改工单行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotSplitApplicationService {

    private static final Set<String> ALIVE = Set.of("NORMAL", "RUNNING");

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final WaferRepository waferRepository;
    private final LotHoldRepository holdRepository;
    private final LotStepPassRepository stepPassRepository;
    private final LotRelationRepository relationRepository;
    private final LotHistoryApplicationService lotHistory;
    private final WaferHistoryApplicationService waferHistory;
    private final GenealogyLinkApplicationService genealogy;
    private final Clock clock;

    /**
     * 从等待中的 Lot 拆出子 Lot。
     *
     * @param lotId 父 Lot
     * @param request 要拆走的片
     * @return 子 Lot 号
     * @throws BusinessException 不是等待状态、没有可拆的片，或父 Lot 会被拆空
     */
    public String split(Long lotId, LotSplitRequest request) {
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("SPLIT");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", lotId)));
        return transactions.execute(command, () -> apply(lotId, request.getWaferIds(), false), String.class);
    }

    /**
     * 把指定的片留在原步骤，拆成子 Lot。父 Lot 可以是等待或加工中。
     *
     * @param lotId 父 Lot
     * @param waferIds 留在原步骤的片
     * @return 子 Lot 号
     */
    public String splitLeaving(Long lotId, List<Long> waferIds) {
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("SPLIT");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", lotId)));
        return transactions.execute(command, () -> apply(lotId, waferIds, true), String.class);
    }

    private String apply(Long lotId, List<Long> waferIds, boolean stay) {
        Lot parent = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (parent.getBatchId() != null) {
            throw new BusinessException(MesExecErrorCodes.BATCH_LOT_BUSY, "批次中的 Lot 不能拆批");
        }
        boolean open = "WAITING".equals(parent.getStatus()) || (stay && "RUNNING".equals(parent.getStatus()));
        if (!open) {
            throw new BusinessException(MesExecErrorCodes.LOT_SPLIT_NOT_WAITING, "只有等待中的 Lot 可以拆批");
        }
        List<Wafer> alive = waferRepository.listByLot(lotId).stream()
                .filter(wafer -> ALIVE.contains(wafer.getStatus()))
                .toList();
        List<Wafer> moving = alive.stream().filter(wafer -> waferIds.contains(wafer.getId())).toList();
        if (moving.isEmpty() || moving.size() != waferIds.size() || moving.size() >= alive.size()) {
            throw new BusinessException(MesExecErrorCodes.LOT_SPLIT_QTY_INVALID, "拆批必须留下至少一片，并且只能拆存活片");
        }
        int seq = lotRepository.maxSplitSeq(lotId) + 1;
        long childInitial = share(parent.getInitialQty(), moving.size(), alive.size());
        Lot child = childOf(parent, seq, moving.size(), childInitial, stay);
        lotRepository.save(child);
        for (Wafer wafer : moving) {
            waferRepository.lock(wafer.getId());
            wafer.setLotId(child.getId());
            waferRepository.update(wafer);
            waferHistory.record(wafer, child, "SPLIT", false, wafer.getSlotNo(), wafer.getStatus(), null, null, null);
        }
        parent.setQty(parent.getQty() - moving.size());
        parent.setWaferCount(parent.getWaferCount() - moving.size());
        parent.setInitialQty((parent.getInitialQty() == null ? 0 : parent.getInitialQty()) - childInitial);
        lotRepository.update(parent);
        copyPasses(parent.getId(), child.getId());
        copyHolds(parent, child);
        relation(parent, child, moving.size());
        genealogy.link("LOT", parent.getId(), parent.getLotNo(), "LOT", child.getId(), child.getLotNo(),
                "SPLIT", parent.getStepId(), BigDecimal.valueOf(moving.size()), null);
        lotHistory.record(parent, "SPLIT", null, null, null, parent.getStatus(), parent.getQty() + moving.size());
        if (parent.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", parent.getWoId(), null,
                    "{\"wo_id\":" + parent.getWoId() + "}");
        }
        return child.getLotNo();
    }

    private Lot childOf(Lot parent, int seq, int qty, long initial, boolean stay) {
        Lot child = new Lot();
        child.setLotNo(parent.getLotNo() + "." + seq);
        child.setLotType(parent.getLotType());
        child.setWoId(parent.getWoId());
        child.setParentLotId(parent.getId());
        child.setRootLotId(parent.getRootLotId() == null ? parent.getId() : parent.getRootLotId());
        child.setSplitSeq(seq);
        child.setReworkId(parent.getReworkId());
        child.setReworkSeqNo(parent.getReworkSeqNo());
        child.setProductId(parent.getProductId());
        child.setProductVersionId(parent.getProductVersionId());
        child.setRouteVersionId(parent.getRouteVersionId());
        child.setBomVersionId(parent.getBomVersionId());
        child.setRouteSeqNo(parent.getRouteSeqNo());
        child.setStepId(parent.getStepId());
        child.setPassNo(parent.getPassNo());
        child.setStatus(stay ? parent.getStatus() : "WAITING");
        if (stay) {
            child.setEquipmentId(parent.getEquipmentId());
            child.setPortId(parent.getPortId());
            child.setRecipeVersionId(parent.getRecipeVersionId());
            child.setTrackInAt(parent.getTrackInAt());
            child.setTrackInBy(parent.getTrackInBy());
            child.setLocationType(parent.getLocationType());
            child.setLocationId(parent.getLocationId());
        }
        child.setHoldCount(0);
        child.setQty((long) qty);
        child.setQtyUnitId(parent.getQtyUnitId());
        child.setInitialQty(initial);
        child.setScrapQty(0L);
        child.setCurrentScrapQty(0L);
        child.setWaferCount(qty);
        child.setPriority(parent.getPriority());
        child.setIsHot(parent.getIsHot());
        child.setStepEnterAt(parent.getStepEnterAt());
        child.setLastTrackOutAt(parent.getLastTrackOutAt());
        child.setVersion(0);
        return child;
    }

    private void copyPasses(Long parentId, Long childId) {
        for (LotStepPass pass : stepPassRepository.listByLot(parentId)) {
            LotStepPass copy = new LotStepPass();
            copy.setLotId(childId);
            copy.setStepId(pass.getStepId());
            copy.setPassCount(pass.getPassCount());
            stepPassRepository.save(copy);
        }
    }

    private void copyHolds(Lot parent, Lot child) {
        List<LotHold> active = holdRepository.listActive(parent.getId());
        for (LotHold hold : active) {
            LotHold copy = new LotHold();
            copy.setLotId(child.getId());
            copy.setReasonCode(hold.getReasonCode());
            copy.setHoldType(hold.getHoldType());
            copy.setHoldSource(hold.getHoldSource());
            copy.setStatus("ACTIVE");
            copy.setActiveKey(rewriteKey(hold.getActiveKey(), parent.getId(), child.getId()));
            copy.setHoldStepId(hold.getHoldStepId());
            copy.setHoldSeqNo(hold.getHoldSeqNo());
            copy.setHoldBy(hold.getHoldBy());
            copy.setHoldAt(hold.getHoldAt());
            copy.setHoldRemark(hold.getHoldRemark());
            copy.setReleaseRoleCode(hold.getReleaseRoleCode());
            copy.setSourceRefId(hold.getSourceRefId());
            holdRepository.save(copy);
        }
        child.setHoldCount(active.size());
        if (!active.isEmpty()) {
            lotRepository.update(child);
        }
    }

    private void relation(Lot parent, Lot child, int qty) {
        LotRelation relation = new LotRelation();
        relation.setParentLotId(parent.getId());
        relation.setChildLotId(child.getId());
        relation.setRelationType("SPLIT");
        relation.setQty((long) qty);
        relation.setUnitId(parent.getQtyUnitId());
        relation.setOccurredAt(LocalDateTime.now(clock));
        relationRepository.save(relation);
    }

    private static long share(Long initial, int moved, int alive) {
        long total = initial == null ? 0 : initial;
        return alive == 0 ? 0 : total * moved / alive;
    }

    private static String rewriteKey(String key, Long parentId, Long childId) {
        if (key == null || parentId == null || childId == null) {
            return key;
        }
        String prefix = parentId + "|";
        return key.startsWith(prefix) ? childId + key.substring(parentId.toString().length()) : key;
    }
}
