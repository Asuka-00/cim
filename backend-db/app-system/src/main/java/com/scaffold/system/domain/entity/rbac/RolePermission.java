package com.scaffold.system.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RolePermission 角色权限关联实体
 * <p>
 * 记录角色与权限之间的多对多关联关系。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_role_permission")
public class RolePermission extends NonSoftDeletableEntity {

    /**
     * 角色ID（雪花算法）
     */
    private Long roleId;

    /**
     * 权限ID（雪花算法）
     */
    private Long permissionId;

    /**
     * 创建角色权限关联。
     */
    public static RolePermission of(Long roleId, Long permissionId) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.roleId = roleId;
        rolePermission.permissionId = permissionId;
        return rolePermission;
    }
}
