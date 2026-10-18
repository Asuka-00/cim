package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ProductFamily 产品族
 * <p>
 * 对应 mes_product_family。把同类产品归到一组，产品通过 family_id 引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_product_family")
public class ProductFamily extends NonSoftDeletableEntity {

    /** 产品族编码，租户内唯一。 */
    private String familyCode;

    /** 产品族名称。 */
    private String familyName;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
