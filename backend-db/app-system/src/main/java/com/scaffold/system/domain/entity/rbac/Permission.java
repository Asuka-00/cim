package com.scaffold.system.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.SoftDeletableEntity;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.enums.VisibleEnum;
import com.scaffold.share.enums.auth.PermissionType;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Permission 权限实体
 * <p>
 * 记录系统权限定义，支持菜单、按钮、接口等权限类型，以树形结构组织。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_permission")
public class Permission extends SoftDeletableEntity {

    /**
     * 权限编码
     */
    private String permissionCode;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限描述
     */
    private String permissionDescription;

    /**
     * 权限类型（1:菜单 2:按钮 3:接口等）
     */
    private PermissionType type;

    /**
     * 路由访问路径
     */
    private String path;

    /**
     * 路由名
     */
    private String pathName;

    /**
     * 路由元数据（JSON）
     */
    private String meta;

    /**
     * 视图文件路径
     */
    private String component;

    /**
     * 排序号
     */
    private Integer permissionOrder;

    /**
     * 是否在菜单中显示（0:否 1:是）
     */
    private VisibleEnum visible;

    /**
     * 父ID（雪花算法）
     */
    private Long parentId;

    private StateEnum enabled;
}
