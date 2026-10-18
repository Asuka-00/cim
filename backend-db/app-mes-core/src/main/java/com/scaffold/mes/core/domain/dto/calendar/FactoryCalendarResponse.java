package com.scaffold.mes.core.domain.dto.calendar;

import java.time.LocalDate;

/**
 * FactoryCalendarResponse 某一天的工厂日历
 *
 * @param id 日历
 * @param calDate 日期
 * @param isWorkday 1 表示工作日，0 表示休息日
 * @param shiftCodes 当日班次编码，逗号分隔。还没安排时为空
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record FactoryCalendarResponse(Long id, LocalDate calDate, Integer isWorkday, String shiftCodes) {
}
