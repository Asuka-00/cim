package com.scaffold.mes.base.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UomConversion 物料单位换算
 * <p>
 * 对应 mes_uom_conversion。1 个源单位等于 factor 个目标单位。materialId 为 0 表示不限物料。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_uom_conversion")
public class UomConversion extends NonSoftDeletableEntity {

    /** 源单位。 */
    private Long fromUomId;

    /** 目标单位。 */
    private Long toUomId;

    /** 限定物料。0 表示通用。 */
    private Long materialId;

    /** 换算系数。 */
    private BigDecimal factor;
}
