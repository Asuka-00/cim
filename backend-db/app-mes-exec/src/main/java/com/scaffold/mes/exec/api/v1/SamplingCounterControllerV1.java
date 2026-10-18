package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.SamplingCounterResetRequest;
import com.scaffold.mes.exec.domain.entity.SamplingCounter;
import com.scaffold.mes.exec.domain.service.SamplingCounterApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SamplingCounterControllerV1 抽检计数
 * <p>
 * 手工把计数清零。已写下的抽检决定不删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/sampling-counters")
@RequiredArgsConstructor
public class SamplingCounterControllerV1 {

    private final SamplingCounterApplicationService service;

    /**
     * 清零。
     *
     * @param request 规则、站点和产品
     * @return 固定为 true
     */
    @PostMapping("/reset")
    @AuditLog(operation = "清零抽检计数")
    public Result<Boolean> reset(@Valid @RequestBody SamplingCounterResetRequest request) {
        service.reset(request.getRuleId(), request.getStepId(), request.getRouteStepId(), request.getProductId());
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部计数器。
     *
     * @return 按规则排列
     */
    @GetMapping
    public Result<List<SamplingCounter>> list() {
        return Result.success(service.list());
    }
}
