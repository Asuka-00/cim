package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.ConfirmRuleCreateRequest;
import com.scaffold.mes.base.domain.entity.ConfirmRule;
import com.scaffold.mes.base.domain.service.ConfirmRuleApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ConfirmRuleControllerV1 二次确认规则
 * <p>
 * 查询某个动作当前要不要再次确认。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/confirm-rules")
@RequiredArgsConstructor
public class ConfirmRuleControllerV1 {

    private final ConfirmRuleApplicationService service;

    /**
     * 新建规则。
     *
     * @param request 动作和确认方式
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "新建二次确认规则")
    public Result<Boolean> create(@Valid @RequestBody ConfirmRuleCreateRequest request) {
        service.create(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 查询确认方式。
     *
     * @param actionCode 动作
     * @return NONE、REAUTH 或 SECOND_SIGN
     */
    @GetMapping("/{actionCode}")
    public Result<String> required(@PathVariable String actionCode) {
        return Result.success(service.required(actionCode));
    }

    /**
     * 列出全部规则。
     *
     * @return 按动作编码排列
     */
    @GetMapping
    public Result<List<ConfirmRule>> list() {
        return Result.success(service.list());
    }
}
