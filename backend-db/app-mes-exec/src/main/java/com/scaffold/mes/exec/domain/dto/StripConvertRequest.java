package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * StripConvertRequest 切筋成型
 * <p>
 * 把 Lot 上仍为 NORMAL 的 Strip 标成 CONVERTED，Lot 的主数量改为这些 Strip 的良品合计，单位改为 Unit。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class StripConvertRequest {

    /** Unit 计数单位。 */
    @NotNull
    private Long unitId;
}
