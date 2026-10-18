package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.RouteStepCollectItemSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.service.RouteStepCollectItemApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * RouteStepCollectItemControllerV1 Route 步骤采集规格
 * <p>
 * 保存后，这一步的采集规格替换站点规格。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/route-step-collect-items")
@RequiredArgsConstructor
public class RouteStepCollectItemControllerV1 {

    private final RouteStepCollectItemApplicationService service;

    /**
     * 保存规格。
     *
     * @param request 版本、序号、采集项和上下限
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "保存步骤采集规格")
    public Result<Boolean> save(@Valid @RequestBody RouteStepCollectItemSaveRequest request) {
        service.save(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出某个版本的采集规格。
     *
     * @param routeVersionId Route 版本
     * @return 规格
     */
    @GetMapping
    public Result<List<RouteStepCollectItem>> list(@RequestParam Long routeVersionId) {
        return Result.success(service.list(routeVersionId));
    }
}
