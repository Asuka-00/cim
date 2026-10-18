package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UserScope 用户数据权限
 * <p>
 * 对应 mes_user_scope。某个类型一条都没有时，这个用户不受该类型限制。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_user_scope")
public class UserScope extends NonSoftDeletableEntity {

    /** 用户名。 */
    private String userName;

    /** AREA、STEP 或 EQUIPMENT_GROUP。 */
    private String scopeType;

    /** 范围对象。 */
    private Long scopeId;
}
