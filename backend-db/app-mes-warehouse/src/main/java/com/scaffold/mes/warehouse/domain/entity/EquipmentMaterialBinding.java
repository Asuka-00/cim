package com.scaffold.mes.warehouse.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * EquipmentMaterialBinding 设备物料绑定
 * <p>
 * 对应 mes_equipment_material_binding。ACTIVE 时剩余量可以出站扣减。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_equipment_material_binding")
public class EquipmentMaterialBinding extends NonSoftDeletableEntity {

    /** 设备。 */
    private Long equipmentId;

    /** 物料。 */
    private Long materialId;

    /** 物料批。 */
    private Long materialLotId;

    /** 剩余量。 */
    private BigDecimal remainingQty;

    /** 计量单位。 */
    private Long uomId;

    /** ACTIVE、DEPLETED 或 UNMOUNTED。 */
    private String status;

    /** RETURN_LINESIDE 或 SCRAP。 */
    private String unmountDisposition;

    /** 下料时的剩余量。 */
    private BigDecimal unmountedRemainingQty;
}
