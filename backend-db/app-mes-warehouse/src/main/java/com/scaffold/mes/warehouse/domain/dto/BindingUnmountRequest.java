package com.scaffold.mes.warehouse.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * BindingUnmountRequest 设备物料下料
 * <p>
 * RETURN_LINESIDE 必须带库位。SCRAP 不退回库存。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class BindingUnmountRequest {

    /** RETURN_LINESIDE 或 SCRAP。 */
    @NotBlank
    private String disposition;

    /** 退回的线边库位。 */
    private Long locationId;
}
