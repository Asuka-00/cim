package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotUdaPutRequest 写入 Lot 自定义属性
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotUdaPutRequest {

    /** 属性编码。 */
    @NotBlank
    private String attrCode;

    /** 属性值。 */
    @NotBlank
    private String value;
}
