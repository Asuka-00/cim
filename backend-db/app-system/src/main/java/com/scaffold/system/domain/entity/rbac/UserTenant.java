package com.scaffold.system.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UserTenant 用户-租户成员资格
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-09-11
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_user_tenant")
public class UserTenant extends NonSoftDeletableEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 是否默认租户
     */
    private Boolean isDefault;

    /**
     * 创建成员资格。tenantId 必须显式指定（表已忽略租户拦截器）。
     */
    public static UserTenant of(Long userId, String tenantId, boolean isDefault) {
        UserTenant membership = new UserTenant();
        membership.userId = userId;
        membership.setTenantId(tenantId);
        membership.isDefault = isDefault;
        return membership;
    }
}
