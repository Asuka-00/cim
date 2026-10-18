package com.scaffold.mes.core.domain.repository.calendar;

import java.time.LocalDate;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.calendar.FactoryCalendar;

/**
 * FactoryCalendarRepository 工厂日历仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface FactoryCalendarRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 日历，不存在时为空
     */
    Optional<FactoryCalendar> findById(Long id);

    /**
     * 按日期查询。
     *
     * @param calDate 日期
     * @return 日历，不存在时为空
     */
    Optional<FactoryCalendar> findByDate(LocalDate calDate);

    /**
     * 插入。
     *
     * @param calendar 日历
     */
    void save(FactoryCalendar calendar);

    /**
     * 更新。
     *
     * @param calendar 日历
     */
    void update(FactoryCalendar calendar);
}
