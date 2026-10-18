package com.scaffold.system.domain.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 切换当前租户请求。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-09-11
 */
@Data
public class SwitchTenantRequestDTO {

    @NotBlank(message = "租户ID不能为空")
    private String tenantId;
}
