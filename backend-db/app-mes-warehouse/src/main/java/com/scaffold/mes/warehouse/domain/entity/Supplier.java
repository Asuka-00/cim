package com.scaffold.mes.warehouse.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Supplier 供应商
 * <p>
 * 对应 mes_supplier。编码在租户内唯一。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_supplier")
public class Supplier extends NonSoftDeletableEntity {

    /** 供应商编码。 */
    private String supplierCode;

    /** 供应商名称。 */
    private String supplierName;

    /** 联系人。 */
    private String contact;

    /** 电话。 */
    private String phone;

    /** 1 表示启用。 */
    private Integer enabled;
}
