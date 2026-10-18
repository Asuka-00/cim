package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.StepCollectItemSaveRequest;
import com.scaffold.mes.base.domain.entity.StepCollectItem;
import com.scaffold.mes.base.domain.service.StepCollectItemApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * StepCollectItemControllerV1 站点采集规格
 * <p>
 * 保存上下限，供采集录入判定。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/step-collect-items")
@RequiredArgsConstructor
public class StepCollectItemControllerV1 {

    private final StepCollectItemApplicationService service;

    /**
     * 保存规格。
     *
     * @param request 站点、采集项和上下限
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "保存站点采集规格")
    public Result<Boolean> save(@Valid @RequestBody StepCollectItemSaveRequest request) {
        service.save(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出这个站点的规格。
     *
     * @param stepId 站点
     * @return 规格
     */
    @GetMapping
    public Result<List<StepCollectItem>> list(@RequestParam Long stepId) {
        return Result.success(service.list(stepId));
    }
}
