package com.scaffold.system.domain.dto.rbac;

import java.util.List;

import com.scaffold.share.enums.StateEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * RoleCreateRequestDTO数据传输对象
 * <p>
 * 负责承载RoleCreateRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class RoleCreateRequestDTO {

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 100, message = "角色编码长度不能超过100")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100")
    private String roleName;

    @NotNull(message = "角色状态不能为空")
    private StateEnum enabled;

    private List<Long> permissionIds;
}
