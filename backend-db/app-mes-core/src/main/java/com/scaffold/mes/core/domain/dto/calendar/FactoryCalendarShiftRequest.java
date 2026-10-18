package com.scaffold.mes.core.domain.dto.calendar;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * FactoryCalendarShiftRequest 给某一天安排班次
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class FactoryCalendarShiftRequest {

    /** 班次编码，逗号分隔。 */
    @NotBlank
    private String shiftCodes;
}
