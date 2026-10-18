package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.SamplingRuleCreateRequest;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.service.SamplingRuleApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SamplingRuleControllerV1 抽检规则
 * <p>
 * 建立每 N 个 Lot 抽 1 个的规则。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/sampling-rules")
@RequiredArgsConstructor
public class SamplingRuleControllerV1 {

    private final SamplingRuleApplicationService service;

    /**
     * 新建规则。
     *
     * @param request 编码、名称和间隔
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建抽检规则")
    public Result<Boolean> create(@Valid @RequestBody SamplingRuleCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部规则。
     *
     * @return 按编码排列
     */
    @GetMapping
    public Result<List<SamplingRule>> list() {
        return Result.success(service.list());
    }
}
