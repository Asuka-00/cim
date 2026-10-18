package com.scaffold.system.domain.vo.rbac;

import java.util.List;

import lombok.Data;

/**
 * RolePermissionVO视图对象
 * <p>
 * 负责承载RolePermissionVO相关展示与返回数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class RolePermissionVO {
    /** 该角色已有的权限 */
    private List<PermissionVO> includedPermissions;

    /** 该角色未包含的权限 */
    private List<PermissionVO> excludedPermissions;
}
