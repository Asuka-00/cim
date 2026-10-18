package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotPrintRequest 打印一个 Lot
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotPrintRequest {

    /** 模板编码。 */
    @NotBlank
    private String tplCode;

    /** 份数。空则 1。 */
    private Integer copies;
}
