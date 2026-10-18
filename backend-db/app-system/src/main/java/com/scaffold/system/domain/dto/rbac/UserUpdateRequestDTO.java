package com.scaffold.system.domain.dto.rbac;

import java.util.List;

import com.scaffold.share.enums.StateEnum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UserUpdateRequestDTO数据传输对象
 * <p>
 * 负责承载UserUpdateRequestDTO相关请求与响应字段
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public class UserUpdateRequestDTO {

    @Size(max = 128, message = "显示名长度不能超过128")
    private String displayName;

    @NotNull(message = "用户状态不能为空")
    private StateEnum enabled;

    private Boolean platformAdmin;

    /**
     * 租户成员资格（每个租户各自绑定角色）
     */
    @NotEmpty(message = "至少选择一个租户")
    @Valid
    private List<UserTenantMembershipDTO> memberships;
}
