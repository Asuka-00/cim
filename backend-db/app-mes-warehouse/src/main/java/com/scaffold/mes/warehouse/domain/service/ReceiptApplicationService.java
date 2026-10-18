package com.scaffold.mes.warehouse.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.ReceiptRequest;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.entity.Receipt;
import com.scaffold.mes.warehouse.domain.entity.ReceiptItem;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.mes.warehouse.domain.repository.WarehouseStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.id.AppIdGenerator;

import lombok.RequiredArgsConstructor;

/**
 * ReceiptApplicationService 来料收货
 * <p>
 * 一次确认生成物料批、库位库存和 RECEIVE 流水。需要检验的批停在 PENDING_QC，否则为 AVAILABLE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ReceiptApplicationService {

    private final MesTransactionService transactions;
    private final ReceiptStore store;
    private final WarehouseStore places;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 确认收货。
     *
     * @param request 单号、批号、数量和库位
     * @throws BusinessException 数量不大于 0，或批号已存在
     */
    public void receive(ReceiptRequest request) {
        if (request.getQty() == null || request.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MesWarehouseErrorCodes.RECEIPT_QTY, "收货数量必须大于 0");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("RECEIPT");
        command.setObjectType("MATERIAL_LOT");
        command.setObjectCode(request.getLotCode());
        command.setSource("TERMINAL");
        transactions.execute(command, () -> {
            apply(request);
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(ReceiptRequest request) {
        StorageLocation location = places.findLocationById(request.getLocationId()).orElse(null);
        if (location == null || location.getEnabled() == null || location.getEnabled() != 1) {
            throw new BusinessException(MesWarehouseErrorCodes.LOCATION_DISABLED, "库位不存在或未启用");
        }
        if (store.findLot(request.getLotCode()).isPresent()) {
            throw new BusinessException(MesWarehouseErrorCodes.RECEIPT_LOT_EXISTS, "物料批号已存在");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        MaterialLot lot = new MaterialLot();
        lot.setId(AppIdGenerator.nextId());
        lot.setLotCode(request.getLotCode());
        lot.setMaterialId(request.getMaterialId());
        lot.setReceivedQty(request.getQty());
        lot.setUomId(request.getUomId());
        lot.setStatus(request.isNeedInspect() ? "PENDING_QC" : "AVAILABLE");
        lot.setReceivedAt(now);
        lot.setVersion(0);
        store.saveLot(lot);
        Inventory inventory = new Inventory();
        inventory.setMaterialLotId(lot.getId());
        inventory.setMaterialId(request.getMaterialId());
        inventory.setLocationId(request.getLocationId());
        inventory.setQtyOnHand(request.getQty());
        inventory.setQtyFrozen(BigDecimal.ZERO);
        inventory.setUomId(request.getUomId());
        inventory.setVersion(0);
        store.saveInventory(inventory);
        Receipt receipt = new Receipt();
        receipt.setId(AppIdGenerator.nextId());
        receipt.setReceiptNo(request.getReceiptNo());
        receipt.setStatus("CONFIRMED");
        receipt.setReceivedAt(now);
        store.saveReceipt(receipt);
        ReceiptItem item = new ReceiptItem();
        item.setReceiptId(receipt.getId());
        item.setLineNo(1);
        item.setMaterialId(request.getMaterialId());
        item.setMaterialLotId(lot.getId());
        item.setQty(request.getQty());
        item.setUomId(request.getUomId());
        item.setLocationId(request.getLocationId());
        store.saveItem(item);
        InventoryTxn txn = new InventoryTxn();
        txn.setTxnType("RECEIVE");
        txn.setMaterialLotId(lot.getId());
        txn.setMaterialId(request.getMaterialId());
        txn.setToLocationId(request.getLocationId());
        txn.setQty(request.getQty());
        txn.setUomId(request.getUomId());
        txn.setBalanceAfter(request.getQty());
        txn.setRefType("RECEIPT");
        txn.setRefId(receipt.getId());
        txn.setTxnId(AppIdGenerator.nextId());
        txn.setOperator(session.getCurrentUserNameOrDefault());
        txn.setOccurredAt(now);
        store.saveTxn(txn);
    }
}
