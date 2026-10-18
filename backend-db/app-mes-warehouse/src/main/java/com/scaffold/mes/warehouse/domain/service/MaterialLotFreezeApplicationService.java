package com.scaffold.mes.warehouse.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * MaterialLotFreezeApplicationService 整批冻结
 * <p>
 * 把可用物料批改为 FROZEN。解冻后回到 AVAILABLE，可以再领料。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MaterialLotFreezeApplicationService {

    private final ReceiptStore store;

    /**
     * 整批冻结。
     *
     * @param materialLotId 物料批
     * @throws BusinessException 物料批不是可用状态
     */
    @Transactional
    public void freeze(Long materialLotId) {
        MaterialLot lot = store.findLotById(materialLotId)
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.FREEZE_STATUS, "物料批不存在"));
        if (!"AVAILABLE".equals(lot.getStatus())) {
            throw new BusinessException(MesWarehouseErrorCodes.FREEZE_STATUS, "只有可用物料批可以冻结");
        }
        lot.setStatus("FROZEN");
        store.updateLot(lot);
    }

    /**
     * 解冻。恢复为可用。
     *
     * @param materialLotId 物料批
     * @throws BusinessException 物料批不是冻结状态
     */
    @Transactional
    public void unfreeze(Long materialLotId) {
        MaterialLot lot = store.findLotById(materialLotId)
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.UNFREEZE_STATUS, "物料批不存在"));
        if (!"FROZEN".equals(lot.getStatus())) {
            throw new BusinessException(MesWarehouseErrorCodes.UNFREEZE_STATUS, "只有已冻结的物料批可以解冻");
        }
        lot.setStatus("AVAILABLE");
        store.updateLot(lot);
    }
}
