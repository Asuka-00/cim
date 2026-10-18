package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * UserScopeCreateRequest 授予数据权限
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class UserScopeCreateRequest {

    /** 用户名。 */
    @NotBlank
    private String userName;

    /** AREA、STEP 或 EQUIPMENT_GROUP。 */
    @NotBlank
    private String scopeType;

    /** 范围对象。 */
    @NotNull
    private Long scopeId;
}
