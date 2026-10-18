package com.scaffold.mes.base.domain.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ProductUnitConv 产品计数单位换算
 * <p>
 * 对应 mes_product_unit_conv。1 个源单位等于 factor 个目标单位，实际数量超出 tolerancePct 时需确认或阻断。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_product_unit_conv")
public class ProductUnitConv extends NonSoftDeletableEntity {

    /** 产品。 */
    private Long productId;

    /** 源计数单位。 */
    private Long fromUnitId;

    /** 目标计数单位。 */
    private Long toUnitId;

    /** 换算系数。 */
    private BigDecimal factor;

    /** 允许偏差百分比。 */
    private BigDecimal tolerancePct;

    /** 备注。 */
    private String remark;
}
