package com.scaffold.mes.core.api.v1.shift;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.dto.shift.ShiftCreateRequest;
import com.scaffold.mes.core.domain.entity.shift.Shift;
import com.scaffold.mes.core.domain.service.shift.ShiftApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ShiftControllerV1 班次
 * <p>
 * 新建一个班次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/shifts")
@RequiredArgsConstructor
public class ShiftControllerV1 {

    private final ShiftApplicationService service;

    /**
     * 新建班次。
     *
     * @param request 编码、名称和起止时间
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建班次")
    public Result<Boolean> create(@Valid @RequestBody ShiftCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部班次。
     *
     * @return 按编码排列
     */
    @GetMapping
    public Result<List<Shift>> list() {
        return Result.success(service.list());
    }
}
