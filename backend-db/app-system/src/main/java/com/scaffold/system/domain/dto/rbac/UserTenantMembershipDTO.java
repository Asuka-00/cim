package com.scaffold.system.domain.dto.rbac;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 用户在某个租户下的成员资格与角色。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-09-11
 */
@Data
public class UserTenantMembershipDTO {

    @NotBlank(message = "租户ID不能为空")
    private String tenantId;

    @NotEmpty(message = "每个租户至少选择一个角色")
    private List<Long> roleIds;
}
