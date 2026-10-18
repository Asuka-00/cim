package com.scaffold.mes.core.infra.repository.db.calendar;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.calendar.FactoryCalendar;
import com.scaffold.mes.core.domain.repository.calendar.FactoryCalendarRepository;
import com.scaffold.mes.core.infra.mapper.db.calendar.FactoryCalendarMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * FactoryCalendarDbRepositoryImpl 工厂日历仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class FactoryCalendarDbRepositoryImpl implements FactoryCalendarRepository {

    private final FactoryCalendarMapper mapper;

    @Override
    public Optional<FactoryCalendar> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<FactoryCalendar> findByDate(LocalDate calDate) {
        return new DbRepositorySupport<>(mapper, FactoryCalendar.class)
                .findOne(new LambdaQueryWrapper<FactoryCalendar>().eq(FactoryCalendar::getCalDate, calDate));
    }

    @Override
    public void save(FactoryCalendar calendar) {
        new DbRepositorySupport<>(mapper, FactoryCalendar.class).save(calendar);
    }

    @Override
    public void update(FactoryCalendar calendar) {
        new DbRepositorySupport<>(mapper, FactoryCalendar.class).update(calendar);
    }
}
