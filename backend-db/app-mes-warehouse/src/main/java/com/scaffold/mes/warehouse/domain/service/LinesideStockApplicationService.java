package com.scaffold.mes.warehouse.domain.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.entity.Warehouse;
import com.scaffold.mes.warehouse.infra.mapper.db.InventoryMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.InventoryTxnMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.StorageLocationMapper;
import com.scaffold.mes.warehouse.infra.mapper.db.WarehouseMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.warehouse.response.StockDeduction;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LinesideStockApplicationService 线边扣料
 * <p>
 * 只看线边库房。按库存主键从小到大找第一笔可用量够扣的记录。可用量是在库减去冻结。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LinesideStockApplicationService {

    private final InventoryMapper inventories;
    private final InventoryTxnMapper txns;
    private final StorageLocationMapper locations;
    private final WarehouseMapper warehouses;
    private final Clock clock;

    /**
     * 扣减一种物料，并写一条 CONSUME 流水。
     *
     * @param materialId 物料
     * @param qty 数量
     * @return 被扣的物料批和库位。数量不是正数，或线边不够时为空
     */
    @Transactional
    public StockDeduction deduct(Long materialId, BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            return null;
        }
        List<Inventory> rows = inventories.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getMaterialId, materialId));
        Inventory hit = rows.stream()
                .filter(this::lineside)
                .sorted(Comparator.comparing(Inventory::getId, Comparator.nullsLast(Long::compareTo)))
                .filter(row -> availableOf(row).compareTo(qty) >= 0)
                .findFirst()
                .orElse(null);
        if (hit == null) {
            return null;
        }
        hit.setQtyOnHand(nvl(hit.getQtyOnHand()).subtract(qty));
        new DbRepositorySupport<>(inventories, Inventory.class).update(hit);
        InventoryTxn txn = new InventoryTxn();
        txn.setTxnType("CONSUME");
        txn.setMaterialLotId(hit.getMaterialLotId());
        txn.setMaterialId(materialId);
        txn.setToLocationId(hit.getLocationId());
        txn.setQty(qty.negate());
        txn.setUomId(hit.getUomId());
        txn.setBalanceAfter(hit.getQtyOnHand());
        txn.setOccurredAt(LocalDateTime.now(clock));
        new AppendOnlyRepositorySupport<>(txns, InventoryTxn.class).save(txn);
        return new StockDeduction(hit.getMaterialLotId(), hit.getLocationId(), hit.getUomId());
    }

    /**
     * 把数量加回指定物料批和库位，并写一条正数 CONSUME 流水。
     *
     * @param materialId 物料
     * @param materialLotId 物料批
     * @param locationId 库位
     * @param qty 数量
     * @throws BusinessException 找不到原来的库存
     */
    @Transactional
    public void restore(Long materialId, Long materialLotId, Long locationId, BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            return;
        }
        Inventory hit = inventories.selectList(new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getMaterialId, materialId)
                        .eq(Inventory::getMaterialLotId, materialLotId)
                        .eq(Inventory::getLocationId, locationId))
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(MesWarehouseErrorCodes.MATERIAL_SHORT, "找不到要加回的线边库存"));
        hit.setQtyOnHand(nvl(hit.getQtyOnHand()).add(qty));
        new DbRepositorySupport<>(inventories, Inventory.class).update(hit);
        InventoryTxn txn = new InventoryTxn();
        txn.setTxnType("CONSUME");
        txn.setMaterialLotId(materialLotId);
        txn.setMaterialId(materialId);
        txn.setToLocationId(locationId);
        txn.setQty(qty);
        txn.setUomId(hit.getUomId());
        txn.setBalanceAfter(hit.getQtyOnHand());
        txn.setOccurredAt(LocalDateTime.now(clock));
        new AppendOnlyRepositorySupport<>(txns, InventoryTxn.class).save(txn);
    }

    /**
     * 合计线边可用量。
     *
     * @param materialId 物料
     * @return 在库减去冻结。没有时为 0
     */
    public BigDecimal available(Long materialId) {
        return inventories.selectList(new LambdaQueryWrapper<Inventory>().eq(Inventory::getMaterialId, materialId))
                .stream()
                .filter(this::lineside)
                .map(LinesideStockApplicationService::availableOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean lineside(Inventory row) {
        if (row.getLocationId() == null) {
            return false;
        }
        StorageLocation location = locations.selectById(row.getLocationId());
        if (location == null || location.getWhId() == null) {
            return false;
        }
        Warehouse warehouse = warehouses.selectById(location.getWhId());
        return warehouse != null && "LINESIDE".equals(warehouse.getWhType());
    }

    private static BigDecimal availableOf(Inventory row) {
        return nvl(row.getQtyOnHand()).subtract(nvl(row.getQtyFrozen()));
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
