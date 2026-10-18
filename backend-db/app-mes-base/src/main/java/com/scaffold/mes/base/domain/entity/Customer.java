package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Customer 客户
 * <p>
 * 对应 mes_customer。产品通过 customer_id 记录所属客户。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_customer")
public class Customer extends NonSoftDeletableEntity {

    /** 客户编码，租户内唯一。 */
    private String customerCode;

    /** 客户名称。 */
    private String customerName;

    /** 联系人。 */
    private String contact;

    /** 电话。 */
    private String phone;

    /** 备注。 */
    private String remark;

    /** 1 启用，0 停用。 */
    private Integer enabled;
}
