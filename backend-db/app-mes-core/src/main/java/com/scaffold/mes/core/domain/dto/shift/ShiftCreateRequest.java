package com.scaffold.mes.core.domain.dto.shift;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * ShiftCreateRequest 新建班次
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class ShiftCreateRequest {

    /** 班次编码。 */
    @NotBlank
    private String shiftCode;

    /** 班次名称。 */
    @NotBlank
    private String shiftName;

    /** 开始时间。 */
    @NotNull
    private LocalTime startTime;

    /** 结束时间。 */
    @NotNull
    private LocalTime endTime;
}
