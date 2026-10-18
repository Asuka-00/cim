package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotBonusRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * LotBonusApplicationService 补片
 * <p>
 * 在制 Lot 增加开批数量。有工单时先锁工单，用已开批合计卡计划量。不改工单进度字段，只发进度事件。来料扣减留到 M9。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotBonusApplicationService {

    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WaferRepository waferRepository;
    private final LotRelationRepository relationRepository;
    private final WaferHistoryApplicationService waferHistory;
    private final LotHistoryApplicationService lotHistory;
    private final Clock clock;

    /**
     * 给 Lot 补数量。
     *
     * @param lotId Lot
     * @param request 补入数量
     * @throws BusinessException 数量不合法、Lot 已终态，或超过工单计划量
     */
    public void bonus(Long lotId, LotBonusRequest request) {
        if (request.getQty() == null || request.getQty() <= 0) {
            throw new BusinessException(MesExecErrorCodes.LOT_BONUS_QTY_INVALID, "Bonus 数量必须大于 0");
        }
        Lot loaded = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("BONUS");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        if (loaded.getWoId() != null) {
            locks.add(new MesLockTarget("WORK_ORDER", loaded.getWoId()));
        }
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            apply(lotId, request.getQty());
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long lotId, long qty) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_BONUS_CLOSED, "终态 Lot 不能 Bonus");
        }
        if (lot.getWoId() != null) {
            workOrderRepository.lock(lot.getWoId());
            WorkOrder order = workOrderRepository.findById(lot.getWoId())
                    .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在"));
            if (lotRepository.sumInitialQty(lot.getWoId()) + qty > order.getPlanQty()) {
                throw new BusinessException(MesExecErrorCodes.LOT_BONUS_OVER_PLAN, "Bonus 数量超过工单计划量");
            }
        }
        long before = lot.getQty() == null ? 0 : lot.getQty();
        lot.setQty(before + qty);
        lot.setInitialQty((lot.getInitialQty() == null ? 0 : lot.getInitialQty()) + qty);
        if (lot.getWaferCount() != null && lot.getWaferCount() > 0) {
            int slot = waferRepository.listByLot(lotId).stream().map(Wafer::getSlotNo).filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue).max().orElse(0);
            for (int i = 1; i <= qty; i++) {
                Wafer wafer = new Wafer();
                wafer.setWaferCode(lot.getLotNo() + ".B" + (slot + i));
                wafer.setLotId(lotId);
                wafer.setSlotNo(slot + i);
                wafer.setStatus("NORMAL");
                wafer.setPurpose("PRODUCT");
                wafer.setDummyUseCount(0);
                wafer.setVersion(0);
                waferRepository.save(wafer);
                waferHistory.record(wafer, lot, "CREATE", false, null, null, null, null, null);
            }
            lot.setWaferCount(lot.getWaferCount() + (int) qty);
        }
        lotRepository.update(lot);
        LotRelation relation = new LotRelation();
        relation.setParentLotId(lotId);
        relation.setChildLotId(lotId);
        relation.setRelationType("BONUS");
        relation.setQty(qty);
        relation.setUnitId(lot.getQtyUnitId());
        relation.setOccurredAt(LocalDateTime.now(clock));
        relationRepository.save(relation);
        lotHistory.record(lot, "BONUS", null, null, null, lot.getStatus(), before);
        if (lot.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", lot.getWoId(), null, "{\"wo_id\":" + lot.getWoId() + "}");
        }
    }
}
