package com.scaffold.mes.core.api.v1.notify;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.core.domain.dto.notify.NotifyRuleCreateRequest;
import com.scaffold.mes.core.domain.dto.notify.NotifyRuleResponse;
import com.scaffold.mes.core.domain.service.notify.NotifyRuleApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * NotifyRuleControllerV1 通知规则
 * <p>
 * 新建规则，按事件类型列出已启用的规则，并停用规则。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/notify-rules")
@RequiredArgsConstructor
public class NotifyRuleControllerV1 {

    private final NotifyRuleApplicationService service;

    /**
     * 新建规则。
     *
     * @param request 编码、名称和事件类型
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建通知规则")
    public Result<Boolean> create(@Valid @RequestBody NotifyRuleCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 按事件类型列出已启用的规则。
     *
     * @param eventType 事件类型
     * @return 已启用的规则
     */
    @GetMapping
    public Result<List<NotifyRuleResponse>> listEnabled(@RequestParam String eventType) {
        return Result.success(service.listEnabled(eventType));
    }

    /**
     * 停用规则。
     *
     * @param id 规则
     * @return 固定为 true
     */
    @PostMapping("/{id}/disable")
    @AuditLog(operation = "停用通知规则")
    public Result<Boolean> disable(@PathVariable Long id) {
        service.disable(id);
        return Result.success(Boolean.TRUE);
    }
}
