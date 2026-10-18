package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ConfirmRule 二次确认规则
 * <p>
 * 对应 mes_confirm_rule。停用或没有规则时，动作不需要再次确认。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_confirm_rule")
public class ConfirmRule extends NonSoftDeletableEntity {

    /** 动作编码。 */
    private String actionCode;

    /** NONE、REAUTH 或 SECOND_SIGN。 */
    private String confirmMode;

    /** 第二签名人角色。SECOND_SIGN 时必填。 */
    private String secondRoleCode;

    /** 1 表示启用。 */
    private Integer enabled;
}
