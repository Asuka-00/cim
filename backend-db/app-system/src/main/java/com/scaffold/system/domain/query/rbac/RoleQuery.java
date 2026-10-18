package com.scaffold.system.domain.query.rbac;

import com.scaffold.share.enums.StateEnum;

import lombok.Data;

/**
 * RoleQuery类
 * <p>
 * 负责处理RoleQuery相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class RoleQuery {

    private String roleCode;

    private String roleName;

    private StateEnum enabled;
}
