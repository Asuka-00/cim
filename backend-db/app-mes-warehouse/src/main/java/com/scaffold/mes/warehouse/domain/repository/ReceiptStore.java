package com.scaffold.mes.warehouse.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.scaffold.mes.warehouse.domain.entity.IncomingInspection;
import com.scaffold.mes.warehouse.domain.entity.IncomingWafer;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.entity.Receipt;
import com.scaffold.mes.warehouse.domain.entity.ReceiptItem;

/**
 * ReceiptStore 收货写入
 * <p>
 * 一次收货要写物料批、库存、流水和收货单。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ReceiptStore {

    /**
     * 按批号查询物料批。
     *
     * @param lotCode 批号
     * @return 物料批，不存在时为空
     */
    Optional<MaterialLot> findLot(String lotCode);

    /**
     * 插入物料批。
     *
     * @param lot 物料批
     */
    void saveLot(MaterialLot lot);

    /**
     * 按主键查询物料批。
     *
     * @param id 物料批
     * @return 物料批，不存在时为空
     */
    Optional<MaterialLot> findLotById(Long id);

    /**
     * 列出到期日早于指定日期、且仍是可用或待检的物料批。
     *
     * @param today 今天
     * @return 待过期的物料批
     */
    List<MaterialLot> listDue(LocalDate today);

    /**
     * 列出物料批。
     *
     * @return 全部物料批，没有时为空列表
     */
    List<MaterialLot> listLots();

    /**
     * 更新物料批。检验后改状态和结论。
     *
     * @param lot 已加载的物料批
     */
    void updateLot(MaterialLot lot);

    /**
     * 插入检验记录。
     *
     * @param inspection 检验
     */
    void saveInspection(IncomingInspection inspection);

    /**
     * 同一物料批里是否已有这个来料编号。
     *
     * @param materialLotId 物料批
     * @param vendorWaferId 来料编号
     * @return 已有时为 true
     */
    boolean incomingExists(Long materialLotId, String vendorWaferId);

    /**
     * 插入来料 Wafer。
     *
     * @param wafer 来料片
     */
    void saveIncoming(IncomingWafer wafer);

    /**
     * 列出这个物料批上的来料片。
     *
     * @param materialLotId 物料批
     * @return 来料片，没有时为空列表
     */
    List<IncomingWafer> listIncoming(Long materialLotId);

    /**
     * 插入库存。
     *
     * @param inventory 库存
     */
    void saveInventory(Inventory inventory);

    /**
     * 按物料批和库位查询库存。
     *
     * @param materialLotId 物料批
     * @param locationId 库位
     * @return 库存，不存在时为空
     */
    Optional<Inventory> findInventory(Long materialLotId, Long locationId);

    /**
     * 列出库存。
     *
     * @return 全部库存行，没有时为空列表
     */
    List<Inventory> listInventories();

    /**
     * 更新库存。
     *
     * @param inventory 已加载的库存
     */
    void updateInventory(Inventory inventory);

    /**
     * 插入流水。
     *
     * @param txn 流水
     */
    void saveTxn(InventoryTxn txn);

    /**
     * 插入收货单。
     *
     * @param receipt 收货单
     */
    void saveReceipt(Receipt receipt);

    /**
     * 插入收货明细。
     *
     * @param item 明细
     */
    void saveItem(ReceiptItem item);
}
