package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * CarrierCreateRequest 新建载具
 * <p>
 * 类型空则按 FOUP。FRAME 留给片级绑定，这里不接收。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class CarrierCreateRequest {

    /** 载具编码。 */
    @NotBlank
    private String carrierCode;

    /** 类型。 */
    private String carrierType;

    /** 容量。空则 25。 */
    private Integer capacity;
}
