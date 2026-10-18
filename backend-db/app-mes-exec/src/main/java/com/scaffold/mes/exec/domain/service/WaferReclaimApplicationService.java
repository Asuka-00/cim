package com.scaffold.mes.exec.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.WaferReenterRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * WaferReclaimApplicationService Wafer 回收
 * <p>
 * 报废片，或已终止 Lot 上的片，可以标成 RECLAIMED。再入厂时新建一片，新编号指向原片，客户编号不变。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WaferReclaimApplicationService {

    private final MesTransactionService transactions;
    private final WaferRepository waferRepository;
    private final LotRepository lotRepository;
    private final WaferHistoryApplicationService history;

    /**
     * 把一片标成已回收。
     *
     * @param waferId Wafer
     * @throws BusinessException 片不是报废，且所属 Lot 也没有终止
     */
    public void reclaim(Long waferId) {
        Wafer loaded = require(waferId);
        run(loaded.getLotId(), () -> {
            Wafer wafer = require(waferId);
            Lot lot = lotRepository.findById(wafer.getLotId())
                    .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
            if (!"SCRAPPED".equals(wafer.getStatus()) && !"TERMINATED".equals(lot.getStatus())) {
                throw new BusinessException(MesExecErrorCodes.WAFER_RECLAIM_REJECTED, "这片 Wafer 现在不能回收");
            }
            waferRepository.lock(waferId);
            String before = wafer.getStatus();
            wafer.setStatus("RECLAIMED");
            waferRepository.update(wafer);
            history.record(wafer, lot, "RECLAIM", false, wafer.getSlotNo(), before, null, null, null);
        });
    }

    /**
     * 用已回收的片生成一片新片，挂到指定 Lot。
     *
     * @param lotId 接收的 Lot
     * @param request 原片和新编号
     * @throws BusinessException 原片未回收，或新编号已存在
     */
    public void reenter(Long lotId, WaferReenterRequest request) {
        if (waferRepository.findByCode(request.getWaferCode()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.WAFER_CODE_EXISTS, "Wafer 编号已存在");
        }
        run(lotId, () -> {
            Wafer source = require(request.getReclaimedWaferId());
            if (!"RECLAIMED".equals(source.getStatus())) {
                throw new BusinessException(MesExecErrorCodes.WAFER_REENTER_REJECTED, "只有已回收的片可以再入厂");
            }
            Lot lot = lotRepository.findById(lotId)
                    .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
            if (!"WAITING".equals(lot.getStatus()) && !"RUNNING".equals(lot.getStatus())) {
                throw new BusinessException(MesExecErrorCodes.WAFER_REENTER_REJECTED, "只有在制 Lot 可以接收回收片");
            }
            int slot = waferRepository.listByLot(lotId).stream().map(Wafer::getSlotNo).filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue).max().orElse(0) + 1;
            Wafer created = new Wafer();
            created.setWaferCode(request.getWaferCode());
            created.setLotId(lotId);
            created.setSlotNo(slot);
            created.setStatus("NORMAL");
            created.setPurpose(source.getPurpose() == null ? "PRODUCT" : source.getPurpose());
            created.setVendorWaferId(source.getVendorWaferId());
            created.setRecycledFromWaferId(source.getId());
            created.setDummyUseCount(0);
            created.setVersion(0);
            waferRepository.save(created);
            lot.setQty((lot.getQty() == null ? 0 : lot.getQty()) + 1);
            lot.setWaferCount((lot.getWaferCount() == null ? 0 : lot.getWaferCount()) + 1);
            lotRepository.update(lot);
            history.record(created, lot, "CREATE", false, null, null, null, null, null);
        });
    }

    private void run(Long lotId, Runnable work) {
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("RECLAIM");
        command.setObjectType("LOT");
        command.setObjectId(lotId);
        command.setSource("TERMINAL");
        command.setLocks(List.of(new MesLockTarget("LOT", lotId)));
        transactions.execute(command, () -> {
            work.run();
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private Wafer require(Long id) {
        return waferRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在"));
    }
}
