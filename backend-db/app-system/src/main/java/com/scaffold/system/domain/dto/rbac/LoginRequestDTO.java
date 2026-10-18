package com.scaffold.system.domain.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * LoginRequestDTO数据传输对象
 * <p>
 * 负责承载LoginRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class LoginRequestDTO {

    @NotBlank(message = "用户账号不能为空")
    private String userName;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "租户ID不能为空")
    private String tenantId;
}
