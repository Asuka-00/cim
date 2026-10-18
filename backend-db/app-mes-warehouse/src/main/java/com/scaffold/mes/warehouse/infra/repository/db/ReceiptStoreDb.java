package com.scaffold.mes.warehouse.infra.repository.db;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.warehouse.domain.entity.IncomingInspection;
import com.scaffold.mes.warehouse.domain.entity.IncomingWafer;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.entity.Receipt;
import com.scaffold.mes.warehouse.domain.entity.ReceiptItem;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.mes.warehouse.infra.mapper.db.IncomingInspectionMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.IncomingWaferMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.InventoryMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.InventoryTxnMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.MaterialLotMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.ReceiptItemMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.ReceiptMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ReceiptStoreDb 收货写入实现
 * <p>
 * 流水只插入。物料批、库存和收货单走普通保存。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ReceiptStoreDb implements ReceiptStore {

    private final MaterialLotMapper materialLots;
    private final IncomingInspectionMapper inspections;
    private final IncomingWaferMapper incomingWafers;
    private final InventoryMapper inventories;
    private final InventoryTxnMapper txns;
    private final ReceiptMapper receipts;
    private final ReceiptItemMapper items;

    @Override
    public Optional<MaterialLot> findLot(String lotCode) {
        return new DbRepositorySupport<>(materialLots, MaterialLot.class)
                .findOne(new LambdaQueryWrapper<MaterialLot>().eq(MaterialLot::getLotCode, lotCode));
    }

    @Override
    public void saveLot(MaterialLot lot) {
        new DbRepositorySupport<>(materialLots, MaterialLot.class).save(lot);
    }

    @Override
    public Optional<MaterialLot> findLotById(Long id) {
        return new DbRepositorySupport<>(materialLots, MaterialLot.class).findById(id);
    }

    @Override
    public List<MaterialLot> listDue(LocalDate today) {
        return materialLots.selectList(new LambdaQueryWrapper<MaterialLot>()
                .lt(MaterialLot::getExpireDate, today)
                .in(MaterialLot::getStatus, "AVAILABLE", "PENDING_QC"));
    }

    @Override
    public List<MaterialLot> listLots() {
        return materialLots.selectList(new LambdaQueryWrapper<MaterialLot>().orderByAsc(MaterialLot::getLotCode));
    }

    @Override
    public void updateLot(MaterialLot lot) {
        new DbRepositorySupport<>(materialLots, MaterialLot.class).update(lot);
    }

    @Override
    public void saveInspection(IncomingInspection inspection) {
        new DbRepositorySupport<>(inspections, IncomingInspection.class).save(inspection);
    }

    @Override
    public boolean incomingExists(Long materialLotId, String vendorWaferId) {
        return new DbRepositorySupport<>(incomingWafers, IncomingWafer.class).findOne(new LambdaQueryWrapper<IncomingWafer>()
                .eq(IncomingWafer::getMaterialLotId, materialLotId)
                .eq(IncomingWafer::getVendorWaferId, vendorWaferId)).isPresent();
    }

    @Override
    public void saveIncoming(IncomingWafer wafer) {
        new DbRepositorySupport<>(incomingWafers, IncomingWafer.class).save(wafer);
    }

    @Override
    public List<IncomingWafer> listIncoming(Long materialLotId) {
        return incomingWafers.selectList(new LambdaQueryWrapper<IncomingWafer>()
                .eq(IncomingWafer::getMaterialLotId, materialLotId)
                .orderByAsc(IncomingWafer::getSlotNo));
    }

    @Override
    public void saveInventory(Inventory inventory) {
        new DbRepositorySupport<>(inventories, Inventory.class).save(inventory);
    }

    @Override
    public Optional<Inventory> findInventory(Long materialLotId, Long locationId) {
        return new DbRepositorySupport<>(inventories, Inventory.class).findOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialLotId, materialLotId)
                .eq(Inventory::getLocationId, locationId));
    }

    @Override
    public List<Inventory> listInventories() {
        return inventories.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public void updateInventory(Inventory inventory) {
        new DbRepositorySupport<>(inventories, Inventory.class).update(inventory);
    }

    @Override
    public void saveTxn(InventoryTxn txn) {
        new AppendOnlyRepositorySupport<>(txns, InventoryTxn.class).save(txn);
    }

    @Override
    public void saveReceipt(Receipt receipt) {
        new DbRepositorySupport<>(receipts, Receipt.class).save(receipt);
    }

    @Override
    public void saveItem(ReceiptItem item) {
        new DbRepositorySupport<>(items, ReceiptItem.class).save(item);
    }
}
