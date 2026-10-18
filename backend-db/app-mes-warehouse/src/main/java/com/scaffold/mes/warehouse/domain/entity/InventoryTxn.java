package com.scaffold.mes.warehouse.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * InventoryTxn 库存流水
 * <p>
 * 对应 hist_mes_inventory_txn。只追加。收货写 RECEIVE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_inventory_txn")
public class InventoryTxn extends AppendOnlyEntity {

    /** 事务类型。 */
    private String txnType;

    /** 物料批。 */
    private Long materialLotId;

    /** 物料。 */
    private Long materialId;

    /** 目标库位。 */
    private Long toLocationId;

    /** 变动数量。 */
    private BigDecimal qty;

    /** 计量单位。 */
    private Long uomId;

    /** 变动后在库量。 */
    private BigDecimal balanceAfter;

    /** 关联单据类型。 */
    private String refType;

    /** 关联单据。 */
    private Long refId;

    /** 事务号。 */
    private Long txnId;

    /** 操作人。 */
    private String operator;

    /** 发生时间。 */
    private LocalDateTime occurredAt;
}
