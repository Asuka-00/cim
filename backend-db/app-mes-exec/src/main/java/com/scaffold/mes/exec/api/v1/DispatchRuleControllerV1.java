package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.DispatchRuleRequest;
import com.scaffold.mes.exec.domain.entity.DispatchRule;
import com.scaffold.mes.exec.domain.service.DispatchRuleApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * DispatchRuleControllerV1 作业清单排序规则
 * <p>
 * 新建全局规则。作业清单用 ruleId 指定，不指定时用默认规则。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/dispatch-rules")
@RequiredArgsConstructor
public class DispatchRuleControllerV1 {

    private final DispatchRuleApplicationService service;

    /**
     * 新建规则。
     *
     * @param request 编码和排序键
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建排序规则")
    public Result<Boolean> create(@Valid @RequestBody DispatchRuleRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部规则。
     *
     * @return 按编码排列
     */
    @GetMapping
    public Result<List<DispatchRule>> list() {
        return Result.success(service.list());
    }
}
