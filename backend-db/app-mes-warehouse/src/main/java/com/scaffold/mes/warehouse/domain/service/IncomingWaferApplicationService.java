package com.scaffold.mes.warehouse.domain.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.IncomingWaferLine;
import com.scaffold.mes.warehouse.domain.entity.IncomingWafer;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * IncomingWaferApplicationService 来料 Wafer 清单
 * <p>
 * 挂到已有物料批上，状态为 AVAILABLE。同一批里编号不能重复。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class IncomingWaferApplicationService {

    private final ReceiptStore store;

    /**
     * 录入来料片。
     *
     * @param materialLotId 物料批
     * @param lines 编号和槽位
     * @throws BusinessException 物料批不存在，或编号为空、重复
     */
    @Transactional
    public void record(Long materialLotId, List<IncomingWaferLine> lines) {
        store.findLotById(materialLotId)
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.INSPECTION_STATUS, "物料批不存在"));
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(MesWarehouseErrorCodes.INCOMING_WAFER_CODE, "至少要有一条来料 Wafer");
        }
        HashSet<String> seen = new HashSet<>();
        for (IncomingWaferLine line : lines) {
            if (line.getVendorWaferId() == null || line.getVendorWaferId().isBlank() || !seen.add(line.getVendorWaferId())) {
                throw new BusinessException(MesWarehouseErrorCodes.INCOMING_WAFER_CODE, "来料 Wafer 编号不能为空或重复");
            }
            if (store.incomingExists(materialLotId, line.getVendorWaferId())) {
                throw new BusinessException(MesWarehouseErrorCodes.INCOMING_WAFER_EXISTS, "来料 Wafer 编号已存在");
            }
            IncomingWafer wafer = new IncomingWafer();
            wafer.setMaterialLotId(materialLotId);
            wafer.setVendorWaferId(line.getVendorWaferId());
            wafer.setSlotNo(line.getSlotNo());
            wafer.setStatus("AVAILABLE");
            store.saveIncoming(wafer);
        }
    }

    /**
     * 列出这个物料批上的来料片。
     *
     * @param materialLotId 物料批
     * @return 来料片，没有时为空列表
     */
    public List<IncomingWafer> list(Long materialLotId) {
        return store.listIncoming(materialLotId);
    }
}
