package com.scaffold.mes.warehouse.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Inventory 库存
 * <p>
 * 对应 mes_inventory。按物料批和库位记账。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_inventory")
public class Inventory extends NonSoftDeletableEntity {

    /** 物料批。 */
    private Long materialLotId;

    /** 物料。 */
    private Long materialId;

    /** 库位。 */
    private Long locationId;

    /** 在库数量。 */
    private BigDecimal qtyOnHand;

    /** 冻结数量。 */
    private BigDecimal qtyFrozen;

    /** 计量单位。 */
    private Long uomId;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
