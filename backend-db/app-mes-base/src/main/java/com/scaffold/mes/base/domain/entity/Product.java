package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Product 产品
 * <p>
 * 对应 mes_product。M1 只保存静态料号，版本和工艺路线在 M2。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_product")
public class Product extends NonSoftDeletableEntity {

    /** 产品料号，租户内唯一。 */
    private String productCode;

    /** 产品名称。 */
    private String productName;

    /** 类型：WAFER、DIE、PACKAGE、OTHER。 */
    private String productType;

    /** 1 表示管控 Unit 序列号。 */
    private Integer trackSerial;

    /** 产品族，可空。 */
    private Long familyId;

    /** 客户，可空。 */
    private Long customerId;

    /** 客户料号。 */
    private String customerPartNo;

    /** 晶圆尺寸，毫米。只允许 150、200、300。 */
    private Integer waferSizeMm;

    /** 每片晶圆的毛 Die 数。 */
    private Integer grossDiePerWafer;

    /** 开批默认计数单位。 */
    private Long baseUnitId;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
