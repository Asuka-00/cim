package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * StripCreateRequest 建立 Strip
 * <p>
 * 挂到在制 Lot 上，并把 Unit 数加进 Lot 的副数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StripCreateRequest {

    /** Strip 编号。 */
    @NotBlank
    private String stripCode;

    /** 含 Unit 数，至少 1。 */
    @NotNull
    private Integer unitQty;

    /** 良品数。空则等于含 Unit 数。 */
    private Integer goodQty;
}
