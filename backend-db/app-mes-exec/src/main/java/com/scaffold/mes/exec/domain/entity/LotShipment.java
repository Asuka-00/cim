package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * LotShipment 出货记录
 * <p>
 * 对应 mes_lot_shipment。出货不可撤销。一张出货单号在租户内只使用一次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot_shipment")
public class LotShipment extends NonSoftDeletableEntity {

    /** 出货单号。 */
    private String shipNo;

    /** Lot。 */
    private Long lotId;

    /** 工单。 */
    private Long woId;

    /** 客户。 */
    private Long customerId;

    /** 出货数量。 */
    private Long qty;

    /** 计数单位。 */
    private Long unitId;

    /** 出货时间。 */
    private LocalDateTime shippedAt;

    /** 出货人。 */
    private String shippedBy;

    /** 备注。 */
    private String remark;
}
