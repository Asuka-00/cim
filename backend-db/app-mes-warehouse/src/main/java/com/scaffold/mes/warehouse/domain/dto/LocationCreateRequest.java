package com.scaffold.mes.warehouse.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * LocationCreateRequest 新建库位
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LocationCreateRequest {

    /** 库房。 */
    @NotNull
    private Long whId;

    /** 编码。 */
    @NotBlank
    private String locCode;

    /** 名称。 */
    private String locName;
}
