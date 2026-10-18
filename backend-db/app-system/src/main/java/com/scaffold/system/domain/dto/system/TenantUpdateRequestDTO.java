package com.scaffold.system.domain.dto.system;

import com.scaffold.share.enums.StateEnum;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TenantUpdateRequestDTO数据传输对象
 * <p>
 * 负责承载TenantUpdateRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class TenantUpdateRequestDTO {

    @Size(max = 200, message = "租户名称长度不能超过200")
    private String tenantName;

    private StateEnum enabled;
}
