package com.scaffold.system.domain.vo.rbac;

import java.util.List;

import lombok.Data;

/**
 * 用户在某个租户下的成员资格与角色。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-09-11
 */
@Data
public class UserTenantMembershipVO {

    private String tenantId;

    private String tenantName;

    private List<String> roleIds;
}
