package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * EquipmentFaultLot 异常影响的 Lot
 * <p>
 * 对应 mes_equipment_fault_lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_equipment_fault_lot")
public class EquipmentFaultLot extends NonSoftDeletableEntity {

    /** 异常单。 */
    private Long faultId;

    /** Lot。 */
    private Long lotId;
}
