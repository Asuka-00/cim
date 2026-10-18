package com.scaffold.mes.exec.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferHistoryRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;

import lombok.RequiredArgsConstructor;

/**
 * WaferUnscrapApplicationService 取消报废
 * <p>
 * 把报废片恢复到报废前的状态，并把数量加回 Lot。Lot 已整批报废时，按报废前快照回到原来的在制状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WaferUnscrapApplicationService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final MesTransactionService transactions;
    private final WaferRepository waferRepository;
    private final WaferHistoryRepository waferHistoryRepository;
    private final LotRepository lotRepository;
    private final LotHistoryRepository lotHistoryRepository;
    private final WaferHistoryApplicationService waferHistory;
    private final LotHistoryApplicationService lotHistory;
    private final ConfirmPort confirms;

    /**
     * 取消一片的报废。
     *
     * @param waferId Wafer
     * @throws BusinessException 片不是报废状态
     */
    public void unscrap(Long waferId) {
        Wafer loaded = waferRepository.findById(waferId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在"));
        if (!confirms.allowed("UNSCRAP", "LOT", loaded.getLotId())) {
            throw new BusinessException(MesExecErrorCodes.CONFIRM_REQUIRED, "解报废还没有完成确认");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("UNSCRAP");
        command.setObjectType("LOT");
        command.setObjectId(loaded.getLotId());
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", loaded.getLotId())));
        transactions.execute(command, () -> {
            apply(waferId);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(Long waferId) {
        Wafer wafer = waferRepository.findById(waferId)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在"));
        if (!"SCRAPPED".equals(wafer.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.WAFER_UNSCRAP_STATUS, "只有报废片可以取消报废");
        }
        WaferHistory scrap = waferHistoryRepository.findLatest(waferId, "SCRAP").orElse(null);
        String restored = "NORMAL";
        if (scrap != null && scrap.getBeforeJson() != null) {
            restored = read(scrap.getBeforeJson()).path("status").asText("NORMAL");
        }
        waferRepository.lock(waferId);
        wafer.setStatus(restored);
        wafer.setScrapReason(null);
        wafer.setScrappedStepId(null);
        wafer.setScrappedAt(null);
        waferRepository.update(wafer);
        Lot lot = lotRepository.findById(wafer.getLotId())
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
        if ("SCRAPPED".equals(lot.getStatus())) {
            LotHistory lotScrap = lotHistoryRepository.findLatest(lot.getId(), "SCRAP").orElse(null);
            String status = "WAITING";
            if (lotScrap != null && lotScrap.getBeforeJson() != null) {
                status = read(lotScrap.getBeforeJson()).path("status").asText("WAITING");
            }
            lot.setStatus(status);
            lot.setFinishedAt(null);
        }
        lot.setQty((lot.getQty() == null ? 0 : lot.getQty()) + 1);
        lot.setWaferCount((lot.getWaferCount() == null ? 0 : lot.getWaferCount()) + 1);
        lot.setScrapQty(Math.max(0, (lot.getScrapQty() == null ? 0 : lot.getScrapQty()) - 1));
        lot.setCurrentScrapQty(Math.max(0, (lot.getCurrentScrapQty() == null ? 0 : lot.getCurrentScrapQty()) - 1));
        lotRepository.update(lot);
        waferHistory.record(wafer, lot, "UNSCRAP", false, wafer.getSlotNo(), "SCRAPPED", null, null, null);
        lotHistory.record(lot, "UNSCRAP", null, null, null, "SCRAPPED", lot.getQty() - 1);
        if (lot.getWoId() != null) {
            transactions.publish("WORK_ORDER_PROGRESS", "WORK_ORDER", lot.getWoId(), null, "{\"wo_id\":" + lot.getWoId() + "}");
        }
    }

    private static JsonNode read(String json) {
        try {
            return JSON.readTree(json);
        } catch (Exception ex) {
            return JSON.createObjectNode();
        }
    }
}
