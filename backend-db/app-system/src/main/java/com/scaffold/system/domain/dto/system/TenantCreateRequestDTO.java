package com.scaffold.system.domain.dto.system;

import com.scaffold.share.enums.StateEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TenantCreateRequestDTO数据传输对象
 * <p>
 * 负责承载TenantCreateRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class TenantCreateRequestDTO {

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 64, message = "租户编码长度不能超过64")
    private String tenantId;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 200, message = "租户名称长度不能超过200")
    private String tenantName;

    @NotNull(message = "租户状态不能为空")
    private StateEnum enabled;
}
