package com.scaffold.mes.warehouse.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * WarehouseCreateRequest 新建库房
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WarehouseCreateRequest {

    /** 编码。 */
    @NotBlank
    private String whCode;

    /** 名称。 */
    @NotBlank
    private String whName;

    /** RAW、LINESIDE、QUARANTINE 或 OTHER。空表示 RAW。 */
    private String whType;
}
