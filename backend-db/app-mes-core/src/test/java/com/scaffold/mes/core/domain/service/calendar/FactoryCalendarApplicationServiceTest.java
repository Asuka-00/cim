package com.scaffold.mes.core.domain.service.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarCreateRequest;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarResponse;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarShiftRequest;
import com.scaffold.mes.core.domain.entity.calendar.FactoryCalendar;
import com.scaffold.mes.core.domain.entity.shift.Shift;
import com.scaffold.mes.core.domain.repository.calendar.FactoryCalendarRepository;
import com.scaffold.mes.core.domain.repository.shift.ShiftRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 为一天新建工厂日历。同一天不能重复。可以按日期查出工作日标记和班次。班次编码必须已经存在。
 */
class FactoryCalendarApplicationServiceTest {

    private FactoryCalendarRepository repository;
    private ShiftRepository shifts;
    private FactoryCalendarApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(FactoryCalendarRepository.class);
        shifts = mock(ShiftRepository.class);
        when(repository.findByDate(LocalDate.parse("2026-10-16"))).thenReturn(Optional.empty());
        service = new FactoryCalendarApplicationService(repository, shifts);
    }

    @Test
    void createsWorkday() {
        service.create(request(1));

        verify(repository).save(org.mockito.ArgumentMatchers.<FactoryCalendar>argThat(row ->
                LocalDate.parse("2026-10-16").equals(row.getCalDate()) && Integer.valueOf(1).equals(row.getIsWorkday())));
    }

    @Test
    void createsRestDay() {
        service.create(request(0));

        verify(repository).save(org.mockito.ArgumentMatchers.<FactoryCalendar>argThat(row ->
                Integer.valueOf(0).equals(row.getIsWorkday())));
    }

    @Test
    void duplicateDateRejected() {
        when(repository.findByDate(LocalDate.parse("2026-10-16"))).thenReturn(Optional.of(new FactoryCalendar()));

        assertThatThrownBy(() -> service.create(request(1)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.CALENDAR_INVALID);
    }

    @Test
    void badFlagRejected() {
        FactoryCalendarCreateRequest request = request(2);

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.CALENDAR_INVALID);
    }

    @Test
    void assignsExistingShiftCodes() {
        when(repository.findById(7L)).thenReturn(Optional.of(new FactoryCalendar()));
        when(shifts.findByCode("DAY")).thenReturn(Optional.of(new Shift()));
        when(shifts.findByCode("NIGHT")).thenReturn(Optional.of(new Shift()));
        FactoryCalendarShiftRequest request = new FactoryCalendarShiftRequest();
        request.setShiftCodes(" DAY, NIGHT ");

        service.assignShifts(7L, request);

        verify(repository).update(org.mockito.ArgumentMatchers.<FactoryCalendar>argThat(row ->
                "DAY,NIGHT".equals(row.getShiftCodes())));
    }

    @Test
    void unknownShiftCodeRejected() {
        when(repository.findById(7L)).thenReturn(Optional.of(new FactoryCalendar()));
        when(shifts.findByCode("DAY")).thenReturn(Optional.empty());
        FactoryCalendarShiftRequest request = new FactoryCalendarShiftRequest();
        request.setShiftCodes("DAY");

        assertThatThrownBy(() -> service.assignShifts(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.SHIFT_INVALID);
    }

    @Test
    void showsWorkdayAndShifts() {
        FactoryCalendar calendar = new FactoryCalendar();
        calendar.setCalDate(LocalDate.parse("2026-10-16"));
        calendar.setIsWorkday(1);
        calendar.setShiftCodes("DAY,NIGHT");
        when(repository.findByDate(LocalDate.parse("2026-10-16"))).thenReturn(Optional.of(calendar));

        FactoryCalendarResponse response = service.show(LocalDate.parse("2026-10-16"));

        assertThat(response.isWorkday()).isEqualTo(1);
        assertThat(response.shiftCodes()).isEqualTo("DAY,NIGHT");
    }

    @Test
    void showRejectsMissingDate() {
        assertThatThrownBy(() -> service.show(LocalDate.parse("2026-10-16")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.CALENDAR_INVALID);
    }

    private static FactoryCalendarCreateRequest request(int workday) {
        FactoryCalendarCreateRequest request = new FactoryCalendarCreateRequest();
        request.setCalDate(LocalDate.parse("2026-10-16"));
        request.setIsWorkday(workday);
        return request;
    }
}
