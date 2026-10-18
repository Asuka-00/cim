package com.scaffold.mes.warehouse.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.InventoryFreezeRequest;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.id.AppIdGenerator;

import lombok.RequiredArgsConstructor;

/**
 * InventoryFreezeApplicationService 库位部分冻结
 * <p>
 * 冻结数量不能超过该库位的在库数量。领料只使用在库减去冻结后的可用量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class InventoryFreezeApplicationService {

    private final ReceiptStore store;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 冻结一部分数量。
     *
     * @param request 物料批、库位和数量
     * @throws BusinessException 没有库存，或冻结后超过在库数量
     */
    @Transactional
    public void freeze(InventoryFreezeRequest request) {
        if (request.getQty() == null || request.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MesWarehouseErrorCodes.INVENTORY_FREEZE_QTY, "冻结数量必须大于 0");
        }
        Inventory inventory = store.findInventory(request.getMaterialLotId(), request.getLocationId())
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.INVENTORY_FREEZE_QTY, "该库位没有库存"));
        BigDecimal frozen = inventory.getQtyFrozen() == null ? BigDecimal.ZERO : inventory.getQtyFrozen();
        BigDecimal next = frozen.add(request.getQty());
        if (inventory.getQtyOnHand() == null || next.compareTo(inventory.getQtyOnHand()) > 0) {
            throw new BusinessException(MesWarehouseErrorCodes.INVENTORY_FREEZE_QTY, "冻结数量超过在库数量");
        }
        inventory.setQtyFrozen(next);
        store.updateInventory(inventory);
        InventoryTxn txn = new InventoryTxn();
        txn.setTxnType("FREEZE");
        txn.setMaterialLotId(request.getMaterialLotId());
        txn.setMaterialId(inventory.getMaterialId());
        txn.setToLocationId(request.getLocationId());
        txn.setQty(request.getQty());
        txn.setUomId(inventory.getUomId());
        txn.setBalanceAfter(inventory.getQtyOnHand());
        txn.setRefType("FREEZE");
        txn.setTxnId(AppIdGenerator.nextId());
        txn.setOperator(session.getCurrentUserNameOrDefault());
        txn.setOccurredAt(LocalDateTime.now(clock));
        store.saveTxn(txn);
    }

    /**
     * 解冻一部分数量。
     *
     * @param request 物料批、库位和数量
     * @throws BusinessException 没有库存，或解冻超过已冻结数量
     */
    @Transactional
    public void unfreeze(InventoryFreezeRequest request) {
        if (request.getQty() == null || request.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MesWarehouseErrorCodes.INVENTORY_UNFREEZE_QTY, "解冻数量必须大于 0");
        }
        Inventory inventory = store.findInventory(request.getMaterialLotId(), request.getLocationId())
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.INVENTORY_UNFREEZE_QTY, "该库位没有库存"));
        BigDecimal frozen = inventory.getQtyFrozen() == null ? BigDecimal.ZERO : inventory.getQtyFrozen();
        if (request.getQty().compareTo(frozen) > 0) {
            throw new BusinessException(MesWarehouseErrorCodes.INVENTORY_UNFREEZE_QTY, "解冻数量超过已冻结数量");
        }
        inventory.setQtyFrozen(frozen.subtract(request.getQty()));
        store.updateInventory(inventory);
        InventoryTxn txn = new InventoryTxn();
        txn.setTxnType("UNFREEZE");
        txn.setMaterialLotId(request.getMaterialLotId());
        txn.setMaterialId(inventory.getMaterialId());
        txn.setToLocationId(request.getLocationId());
        txn.setQty(request.getQty());
        txn.setUomId(inventory.getUomId());
        txn.setBalanceAfter(inventory.getQtyOnHand());
        txn.setRefType("UNFREEZE");
        txn.setTxnId(AppIdGenerator.nextId());
        txn.setOperator(session.getCurrentUserNameOrDefault());
        txn.setOccurredAt(LocalDateTime.now(clock));
        store.saveTxn(txn);
    }
}
