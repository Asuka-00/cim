package com.scaffold.mes.core.domain.service.calendar;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarCreateRequest;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarResponse;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarShiftRequest;
import com.scaffold.mes.core.domain.entity.calendar.FactoryCalendar;
import com.scaffold.mes.core.domain.repository.calendar.FactoryCalendarRepository;
import com.scaffold.mes.core.domain.repository.shift.ShiftRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * FactoryCalendarApplicationService 工厂日历
 * <p>
 * 同一天只能有一行。工作日标记只能是 0 或 1。安排班次时，每个编码都必须已经存在。可以按日期查出工作日标记和班次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class FactoryCalendarApplicationService {

    private final FactoryCalendarRepository repository;

    private final ShiftRepository shifts;

    /**
     * 新建一天的日历。
     *
     * @param request 日期和工作日标记
     * @throws BusinessException 这一天已有日历，日期为空，或标记不是 0 或 1
     */
    @Transactional
    public void create(FactoryCalendarCreateRequest request) {
        if (request.getCalDate() == null
                || request.getIsWorkday() == null
                || (request.getIsWorkday() != 0 && request.getIsWorkday() != 1)) {
            throw new BusinessException(MesErrorCodes.CALENDAR_INVALID, "日期和工作日标记不合法");
        }
        if (repository.findByDate(request.getCalDate()).isPresent()) {
            throw new BusinessException(MesErrorCodes.CALENDAR_INVALID, "这一天已经有日历");
        }
        FactoryCalendar calendar = new FactoryCalendar();
        calendar.setCalDate(request.getCalDate());
        calendar.setIsWorkday(request.getIsWorkday());
        repository.save(calendar);
    }

    /**
     * 给已有的一天写上班次编码。
     *
     * @param id 日历
     * @param request 逗号分隔的班次编码
     * @throws BusinessException 日历不存在，编码为空，或有编码尚未建班次
     */
    @Transactional
    public void assignShifts(Long id, FactoryCalendarShiftRequest request) {
        FactoryCalendar calendar = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.CALENDAR_INVALID, "这一天还没有日历"));
        String joined = joinCodes(request.getShiftCodes());
        calendar.setShiftCodes(joined);
        repository.update(calendar);
    }

    /**
     * 按日期查询。
     *
     * @param calDate 日期
     * @return 工作日标记和班次编码
     * @throws BusinessException 这一天还没有日历
     */
    public FactoryCalendarResponse show(LocalDate calDate) {
        FactoryCalendar calendar = repository.findByDate(calDate)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.CALENDAR_INVALID, "这一天还没有日历"));
        return new FactoryCalendarResponse(calendar.getId(), calendar.getCalDate(), calendar.getIsWorkday(),
                calendar.getShiftCodes());
    }

    private String joinCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(MesErrorCodes.CALENDAR_INVALID, "班次编码不能为空");
        }
        StringBuilder joined = new StringBuilder();
        for (String piece : raw.split(",")) {
            String code = piece.trim();
            if (code.isEmpty()) {
                throw new BusinessException(MesErrorCodes.CALENDAR_INVALID, "班次编码不能为空");
            }
            if (shifts.findByCode(code).isEmpty()) {
                throw new BusinessException(MesErrorCodes.SHIFT_INVALID, "班次编码不存在");
            }
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(code);
        }
        return joined.toString();
    }
}
