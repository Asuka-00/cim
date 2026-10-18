package com.scaffold.mes.warehouse.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.IssueRequest;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.id.AppIdGenerator;

import lombok.RequiredArgsConstructor;

/**
 * IssueApplicationService 领料
 * <p>
 * 只接受 AVAILABLE。从源库位扣减可用数量，加到目标库位，并写 ISSUE 流水。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class IssueApplicationService {

    private final MesTransactionService transactions;
    private final ReceiptStore store;
    private final CurrentSessionProvider session;
    private final Clock clock;

    /**
     * 领料。
     *
     * @param request 物料批、两个库位和数量
     * @throws BusinessException 物料批不可用，或数量超过可用库存
     */
    public void issue(IssueRequest request) {
        if (request.getQty() == null || request.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MesWarehouseErrorCodes.ISSUE_QTY, "领料数量必须大于 0");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("ISSUE");
        command.setObjectType("MATERIAL_LOT");
        command.setObjectId(request.getMaterialLotId());
        command.setSource("TERMINAL");
        transactions.execute(command, () -> {
            apply(request, "ISSUE", "只有可用物料批可以领料");
            return Boolean.TRUE;
        }, Boolean.class);
    }

    /**
     * 退料。from 是当前库位，to 是退回的库位。
     *
     * @param request 物料批、两个库位和数量
     * @throws BusinessException 物料批不可用，或数量超过可用库存
     */
    public void returnMaterial(IssueRequest request) {
        if (request.getQty() == null || request.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MesWarehouseErrorCodes.ISSUE_QTY, "退料数量必须大于 0");
        }
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("RETURN");
        command.setObjectType("MATERIAL_LOT");
        command.setObjectId(request.getMaterialLotId());
        command.setSource("TERMINAL");
        transactions.execute(command, () -> {
            apply(request, "RETURN", "只有可用物料批可以退料");
            return Boolean.TRUE;
        }, Boolean.class);
    }

    private void apply(IssueRequest request, String txnType, String statusMessage) {
        MaterialLot lot = store.findLotById(request.getMaterialLotId())
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.ISSUE_STATUS, "物料批不存在"));
        if (!"AVAILABLE".equals(lot.getStatus())) {
            throw new BusinessException(MesWarehouseErrorCodes.ISSUE_STATUS, statusMessage);
        }
        Inventory source = store.findInventory(lot.getId(), request.getFromLocationId())
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.ISSUE_QTY, "源库位没有库存"));
        BigDecimal frozen = source.getQtyFrozen() == null ? BigDecimal.ZERO : source.getQtyFrozen();
        BigDecimal available = source.getQtyOnHand().subtract(frozen);
        if (available.compareTo(request.getQty()) < 0) {
            throw new BusinessException(MesWarehouseErrorCodes.ISSUE_QTY, "领料数量超过可用库存");
        }
        source.setQtyOnHand(source.getQtyOnHand().subtract(request.getQty()));
        store.updateInventory(source);
        Inventory target = store.findInventory(lot.getId(), request.getToLocationId()).orElse(null);
        if (target == null) {
            target = new Inventory();
            target.setMaterialLotId(lot.getId());
            target.setMaterialId(lot.getMaterialId());
            target.setLocationId(request.getToLocationId());
            target.setQtyOnHand(request.getQty());
            target.setQtyFrozen(BigDecimal.ZERO);
            target.setUomId(lot.getUomId());
            target.setVersion(0);
            store.saveInventory(target);
        } else {
            target.setQtyOnHand(target.getQtyOnHand().add(request.getQty()));
            store.updateInventory(target);
        }
        InventoryTxn txn = new InventoryTxn();
        txn.setTxnType(txnType);
        txn.setMaterialLotId(lot.getId());
        txn.setMaterialId(lot.getMaterialId());
        txn.setToLocationId(request.getToLocationId());
        txn.setQty(request.getQty());
        txn.setUomId(lot.getUomId());
        txn.setBalanceAfter(source.getQtyOnHand());
        txn.setRefType(txnType);
        txn.setTxnId(AppIdGenerator.nextId());
        txn.setOperator(session.getCurrentUserNameOrDefault());
        txn.setOccurredAt(LocalDateTime.now(clock));
        store.saveTxn(txn);
    }
}
