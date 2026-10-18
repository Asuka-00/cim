package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.HandoverConfirmRequest;
import com.scaffold.mes.exec.domain.dto.HandoverCreateRequest;
import com.scaffold.mes.exec.domain.entity.Handover;
import com.scaffold.mes.exec.domain.service.HandoverApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * HandoverControllerV1 交接班
 * <p>
 * 新建草稿，提交，并由接班人确认。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/handovers")
@RequiredArgsConstructor
public class HandoverControllerV1 {

    private final HandoverApplicationService service;

    /**
     * 新建草稿。
     *
     * @param request 单号、交班人和备注
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建交接班")
    public Result<Boolean> create(@Valid @RequestBody HandoverCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 提交草稿。
     *
     * @param id 交接班
     * @return 固定为 true
     */
    @PostMapping("/{id}/submit")
    @AuditLog(operation = "提交交接班")
    public Result<Boolean> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 接班人确认。
     *
     * @param id 交接班
     * @param request 接班人
     * @return 固定为 true
     */
    @PostMapping("/{id}/confirm")
    @AuditLog(operation = "确认交接班")
    public Result<Boolean> confirm(@PathVariable Long id, @Valid @RequestBody HandoverConfirmRequest request) {
        service.confirm(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部交接班。
     *
     * @return 按单号排列
     */
    @GetMapping
    public Result<List<Handover>> list() {
        return Result.success(service.list());
    }
}
