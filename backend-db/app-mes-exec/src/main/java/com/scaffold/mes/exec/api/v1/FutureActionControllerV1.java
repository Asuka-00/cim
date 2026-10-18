package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.FutureActionRequest;
import com.scaffold.mes.exec.domain.entity.FutureAction;
import com.scaffold.mes.exec.domain.service.FutureActionApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * FutureActionControllerV1 预约动作
 * <p>
 * 预约在某个主流程步骤到达时 Hold 或改优先级。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/future-actions")
@RequiredArgsConstructor
public class FutureActionControllerV1 {

    private final FutureActionApplicationService service;

    /**
     * 预约。
     *
     * @param lotId Lot
     * @param request 步骤和动作
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "预约动作")
    public Result<Boolean> schedule(@PathVariable Long lotId, @Valid @RequestBody FutureActionRequest request) {
        service.schedule(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出某个 Lot 的全部预约。
     *
     * @param lotId Lot
     * @return 按步骤序号排列
     */
    @GetMapping
    public Result<List<FutureAction>> list(@PathVariable Long lotId) {
        return Result.success(service.list(lotId));
    }
}
