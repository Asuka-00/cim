package com.scaffold.mes.base.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ToolCreateRequest 新建工具
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ToolCreateRequest {

    /** 编码。 */
    @NotBlank
    private String toolCode;

    /** 名称。 */
    @NotBlank
    private String toolName;

    /** 类型。 */
    @NotNull
    private Long typeId;
}
