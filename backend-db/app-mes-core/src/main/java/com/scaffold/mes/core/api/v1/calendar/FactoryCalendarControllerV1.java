package com.scaffold.mes.core.api.v1.calendar;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarCreateRequest;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarResponse;
import com.scaffold.mes.core.domain.dto.calendar.FactoryCalendarShiftRequest;
import com.scaffold.mes.core.domain.service.calendar.FactoryCalendarApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * FactoryCalendarControllerV1 工厂日历
 * <p>
 * 为一天新建日历，标明是否工作日，写上已有的班次编码，并按日期查询。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/calendars")
@RequiredArgsConstructor
public class FactoryCalendarControllerV1 {

    private final FactoryCalendarApplicationService service;

    /**
     * 新建一天的日历。
     *
     * @param request 日期和工作日标记
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建工厂日历")
    public Result<Boolean> create(@Valid @RequestBody FactoryCalendarCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 给这一天安排班次。
     *
     * @param id 日历
     * @param request 逗号分隔的班次编码
     * @return 固定为 true
     */
    @PostMapping("/{id}/shifts")
    @AuditLog(operation = "安排当日班次")
    public Result<Boolean> assignShifts(@PathVariable Long id, @Valid @RequestBody FactoryCalendarShiftRequest request) {
        service.assignShifts(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 按日期查询。
     *
     * @param date 日期
     * @return 工作日标记和班次编码
     */
    @GetMapping
    public Result<FactoryCalendarResponse> show(@RequestParam LocalDate date) {
        return Result.success(service.show(date));
    }
}
