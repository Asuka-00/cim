package com.scaffold.mes.warehouse.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ReceiptItem 收货明细
 * <p>
 * 对应 mes_receipt_item。一行生成一个物料批。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_receipt_item")
public class ReceiptItem extends NonSoftDeletableEntity {

    /** 收货单。 */
    private Long receiptId;

    /** 行号。 */
    private Integer lineNo;

    /** 物料。 */
    private Long materialId;

    /** 物料批。 */
    private Long materialLotId;

    /** 数量。 */
    private BigDecimal qty;

    /** 计量单位。 */
    private Long uomId;

    /** 库位。 */
    private Long locationId;
}
