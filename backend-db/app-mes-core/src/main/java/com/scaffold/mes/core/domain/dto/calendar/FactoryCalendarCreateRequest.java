package com.scaffold.mes.core.domain.dto.calendar;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * FactoryCalendarCreateRequest 新建工厂日历
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class FactoryCalendarCreateRequest {

    /** 日期。 */
    @NotNull
    private LocalDate calDate;

    /** 1 表示工作日，0 表示休息日。 */
    @NotNull
    private Integer isWorkday;
}
