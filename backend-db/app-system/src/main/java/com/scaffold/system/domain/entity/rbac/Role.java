package com.scaffold.system.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.SoftDeletableEntity;
import com.scaffold.share.enums.StateEnum;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Role 角色实体
 * <p>
 * 记录系统角色定义，包括角色编码、名称、管理员标识和启用状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_role")
public class Role extends SoftDeletableEntity {

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 状态（1:启用 0:禁用）
     */
    private StateEnum enabled;
}
