package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Uom 物料计量单位
 * <p>
 * 对应 mes_uom。表示库存与 BOM 的计量，如 mL、kg，与 Lot 计数单位分开。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_uom")
public class Uom extends NonSoftDeletableEntity {

    /** 单位编码，租户内唯一。 */
    private String uomCode;

    /** 单位名称。 */
    private String uomName;

    /** 量纲：COUNT、VOLUME、WEIGHT、LENGTH、AREA、TIME、OTHER。 */
    private String dimension;

    /** 默认小数位数。 */
    private Integer decimals;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
