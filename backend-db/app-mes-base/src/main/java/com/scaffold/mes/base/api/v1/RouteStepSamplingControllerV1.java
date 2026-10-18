package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.RouteStepSamplingSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.service.RouteStepSamplingApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RouteStepSamplingControllerV1 Route 步骤抽检覆盖
 * <p>
 * 保存后，这一步的抽检规则替换站点绑定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/route-step-samplings")
@RequiredArgsConstructor
public class RouteStepSamplingControllerV1 {

    private final RouteStepSamplingApplicationService service;

    /**
     * 保存覆盖。
     *
     * @param request 版本、序号、产品和规则
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "保存步骤抽检覆盖")
    public Result<Boolean> save(@Valid @RequestBody RouteStepSamplingSaveRequest request) {
        service.save(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出某个版本的抽检覆盖。
     *
     * @param routeVersionId Route 版本
     * @return 覆盖
     */
    @GetMapping
    public Result<List<RouteStepSampling>> list(@RequestParam Long routeVersionId) {
        return Result.success(service.list(routeVersionId));
    }
}
