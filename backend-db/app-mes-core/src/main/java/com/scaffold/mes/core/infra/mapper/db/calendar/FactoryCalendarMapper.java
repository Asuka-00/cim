package com.scaffold.mes.core.infra.mapper.db.calendar;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.calendar.FactoryCalendar;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * FactoryCalendarMapper 工厂日历 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface FactoryCalendarMapper extends BatchBaseMapper<FactoryCalendar> {
}
