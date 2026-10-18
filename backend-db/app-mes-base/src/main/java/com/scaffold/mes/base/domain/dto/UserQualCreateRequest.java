package com.scaffold.mes.base.domain.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * UserQualCreateRequest 新建人员资质
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class UserQualCreateRequest {

    /** 用户名。 */
    @NotBlank
    private String userName;

    /** EQUIPMENT、EQUIPMENT_GROUP 或 STEP。 */
    @NotBlank
    private String targetType;

    /** 目标对象。 */
    @NotNull
    private Long targetId;

    /** 生效止。空表示不限期。 */
    private LocalDateTime validTo;
}
