package com.scaffold.system.domain.query.rbac;

import com.scaffold.share.enums.VisibleEnum;
import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.enums.auth.PermissionType;

import lombok.Data;

/**
 * PermissionQuery类
 * <p>
 * 负责处理PermissionQuery相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class PermissionQuery {

    private String permissionCode;

    private String permissionName;

    private PermissionType type;

    private Long parentId;

    private VisibleEnum visible;

    private StateEnum enabled;
}
