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
import com.scaffold.mes.exec.domain.dto.LotScrapRequest;
import com.scaffold.mes.exec.domain.dto.WaferScrapRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;
import com.scaffold.share.integration.system.DictItemLookup;

import lombok.RequiredArgsConstructor;

/**
 * ScrapApplicationService 报废
 * <p>
 * 片级报废后 Lot 数量等于存活片数。整批报废把存活片全部报废。数量到 0 时 Lot 变为 SCRAPPED。发生过封装投入的源 Lot 归零时变为 CONSUMED。不改工单行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ScrapApplicationService {

    private static final String SCRAP_DICT = "MES_SCRAP_REASON";
    private static final Set<String> OPEN = Set.of("WAITING", "RUNNING");
    private static final Set<String> ALIVE = Set.of("NORMAL", "RUNNING");

    private final MesTransactionService transactions;
    private final LotRepository lotRepository;
    private final WaferRepository waferRepository;
    private final DictItemLookup reasons;
    private final WaferHistoryApplicationService waferHistory;
    private final LotHistoryApplicationService lotHistory;
    private final EquipmentTrackPort equipmentTrack;
    private final LotRelationRepository relationRepository;
    private final ConfirmPort confirms;
    private final Clock clock;

    /**
     * 整批报废。
     *
     * @param lotId Lot
     * @param request 原因码；没有片时还要数量
     * @throws BusinessException 原因无效、Lot 已终态，或数量不合法
     */
    public void scrapLot(Long lotId, LotScrapRequest request) {
        requireReason(request.getReasonCode());
        requireConfirm(lotId);
        run(lotId, () -> applyLot(lotId, request));
    }

    /**
     * 报废一片。
     *
     * @param waferId Wafer
     * @param request 原因码
     * @throws BusinessException 原因无效、片不存在或已不能报废
     */
    public void scrapWafer(Long waferId, WaferScrapRequest request) {
        requireReason(request.getReasonCode());
        Wafer wafer = waferRepository.findById(waferId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在"));
        requireConfirm(wafer.getLotId());
        run(wafer.getLotId(), () -> applyWafer(waferId, request));
    }

    private void run(Long lotId, Runnable work) {
        Lot loaded = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("SCRAP");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        ArrayList<MesLockTarget> locks = new ArrayList<>();
        if (loaded.getEquipmentId() != null) {
            locks.add(new MesLockTarget("EQUIPMENT", loaded.getEquipmentId()));
        }
        locks.add(new MesLockTarget("LOT", lotId));
        command.setLocks(locks);
        transactions.execute(command, () -> {
            work.run();
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void applyLot(Long lotId, LotScrapRequest request) {
        Lot lot = openLot(lotId);
        List<Wafer> alive = alive(lotId);
        if (!alive.isEmpty() || (lot.getWaferCount() != null && lot.getWaferCount() > 0)) {
            if (alive.isEmpty()) {
                throw new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Lot 上没有可报废的 Wafer");
            }
            alive.forEach(wafer -> scrapOne(lot, wafer, request.getReasonCode(), request.getRemark()));
            closeIfEmpty(lot, alive.size(), request.getReasonCode(), request.getRemark());
            return;
        }
        if (request.getScrapQty() == null || request.getScrapQty() <= 0 || request.getScrapQty() > lot.getQty()) {
            throw new BusinessException(MesExecErrorCodes.SCRAP_QTY_INVALID, "报废数量不合法");
        }
        String before = snapshot(lot);
        long qtyBefore = lot.getQty();
        lot.setQty(lot.getQty() - request.getScrapQty());
        addScrap(lot, request.getScrapQty());
        if (lot.getQty() == 0) {
            finish(lot);
        }
        lotRepository.update(lot);
        lotHistory.record(lot, "SCRAP", request.getReasonCode(), request.getRemark(), before, "RUNNING".equals(lot.getStatus()) ? lot.getStatus() : beforeStatus(before), qtyBefore);
        publish(lot);
    }

    private void applyWafer(Long waferId, WaferScrapRequest request) {
        Wafer wafer = waferRepository.findById(waferId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在"));
        Lot lot = openLot(wafer.getLotId());
        scrapOne(lot, wafer, request.getReasonCode(), request.getRemark());
        closeIfEmpty(lot, 1, request.getReasonCode(), request.getRemark());
    }

    private void scrapOne(Lot lot, Wafer wafer, String reason, String remark) {
        if (!lot.getId().equals(wafer.getLotId()) || !ALIVE.contains(wafer.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.WAFER_NOT_SCRAPPABLE, "Wafer 已经不能报废");
        }
        waferRepository.lock(wafer.getId());
        String statusBefore = wafer.getStatus();
        String before = "{\"status\":\"" + statusBefore + "\"}";
        wafer.setStatus("SCRAPPED");
        wafer.setScrapReason(reason);
        wafer.setScrappedStepId(lot.getStepId());
        wafer.setScrappedAt(LocalDateTime.now(clock));
        waferRepository.update(wafer);
        waferHistory.record(wafer, lot, "SCRAP", false, wafer.getSlotNo(), statusBefore, reason, remark, before);
    }

    private void closeIfEmpty(Lot lot, int scrapped, String reason, String remark) {
        String before = snapshot(lot);
        long qtyBefore = lot.getQty() == null ? 0 : lot.getQty();
        long alive = waferRepository.listByLot(lot.getId()).stream().filter(row -> ALIVE.contains(row.getStatus())).count();
        lot.setWaferCount((int) alive);
        lot.setQty(alive);
        addScrap(lot, scrapped);
        if (alive == 0) {
            finish(lot);
        }
        lotRepository.update(lot);
        lotHistory.record(lot, "SCRAP", reason, remark, before, statusOf(before), qtyBefore);
        publish(lot);
    }

    private void finish(Lot lot) {
        Long equipmentId = lot.getEquipmentId();
        long others = equipmentId == null ? 0 : Math.max(0, lotRepository.countRunning(equipmentId) - ("RUNNING".equals(lot.getStatus()) ? 1 : 0));
        lot.setStatus(relationRepository.existsPackageInput(lot.getId()) ? "CONSUMED" : "SCRAPPED");
        lot.setFinishedAt(LocalDateTime.now(clock));
        lot.setEquipmentId(null);
        lot.setPortId(null);
        lot.setRecipeVersionId(null);
        if (equipmentId != null) {
            equipmentTrack.releaseIfIdle(equipmentId, others);
        }
    }

    private void publish(Lot lot) {
        if (lot.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", lot.getWoId(), null, "{\"wo_id\":" + lot.getWoId() + "}");
        }
    }

    private Lot openLot(Long lotId) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if (!OPEN.contains(lot.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.LOT_SCRAP_CLOSED, "终态 Lot 不能报废");
        }
        return lot;
    }

    private List<Wafer> alive(Long lotId) {
        return waferRepository.listByLot(lotId).stream().filter(row -> ALIVE.contains(row.getStatus())).toList();
    }

    private void requireConfirm(Long lotId) {
        if (!confirms.allowed("SCRAP", "LOT", lotId)) {
            throw new BusinessException(MesExecErrorCodes.CONFIRM_REQUIRED, "报废还没有完成确认");
        }
    }

    private void requireReason(String reasonCode) {
        if (reasonCode == null || !reasons.enabled(SCRAP_DICT, reasonCode)) {
            throw new BusinessException(MesExecErrorCodes.SCRAP_REASON_INVALID, "报废原因无效");
        }
    }

    private static void addScrap(Lot lot, long count) {
        lot.setScrapQty((lot.getScrapQty() == null ? 0 : lot.getScrapQty()) + count);
        lot.setCurrentScrapQty((lot.getCurrentScrapQty() == null ? 0 : lot.getCurrentScrapQty()) + count);
    }

    private static String snapshot(Lot lot) {
        return "{\"status\":\"" + lot.getStatus() + "\",\"qty\":" + lot.getQty() + "}";
    }

    private static String statusOf(String before) {
        int start = before.indexOf("\"status\":\"") + 10;
        return before.substring(start, before.indexOf('"', start));
    }

    private static String beforeStatus(String before) {
        return statusOf(before);
    }
}
