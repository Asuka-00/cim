package com.scaffold.mes.warehouse.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * IncomingWafer 来料 Wafer
 * <p>
 * 对应 mes_incoming_wafer。收货时录入，开批前状态是 AVAILABLE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_incoming_wafer")
public class IncomingWafer extends NonSoftDeletableEntity {

    /** 来料物料批。 */
    private Long materialLotId;

    /** 来料 Wafer 编号。 */
    private String vendorWaferId;

    /** 来料槽位。 */
    private Integer slotNo;

    /** AVAILABLE、USED 或 SCRAPPED。 */
    private String status;

    /** 开批后的厂内 Wafer。 */
    private Long waferId;
}
