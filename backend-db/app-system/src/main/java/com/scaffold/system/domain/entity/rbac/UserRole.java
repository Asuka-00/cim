package com.scaffold.system.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * UserRole 用户角色关联实体
 * <p>
 * 记录用户与角色之间的多对多关联关系。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_user_role")
public class UserRole extends NonSoftDeletableEntity {

    /**
     * 用户ID（雪花算法）
     */
    private Long userId;

    /**
     * 角色ID（雪花算法）
     */
    private Long roleId;

    /**
     * 创建用户角色关联。
     */
    public static UserRole of(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.userId = userId;
        userRole.roleId = roleId;
        return userRole;
    }
}
