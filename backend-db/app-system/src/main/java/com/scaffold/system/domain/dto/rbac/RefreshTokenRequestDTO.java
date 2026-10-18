package com.scaffold.system.domain.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RefreshTokenRequestDTO数据传输对象
 * <p>
 * 负责承载RefreshTokenRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class RefreshTokenRequestDTO {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
