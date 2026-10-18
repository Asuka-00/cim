package com.scaffold.mes.base.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.StepSamplingBindRequest;
import com.scaffold.mes.base.domain.entity.StepSampling;
import com.scaffold.mes.base.domain.service.StepSamplingApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * StepSamplingControllerV1 站点抽检绑定
 * <p>
 * 把已启用的抽检规则绑到量测站点。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/step-samplings")
@RequiredArgsConstructor
public class StepSamplingControllerV1 {

    private final StepSamplingApplicationService service;

    /**
     * 绑定规则。
     *
     * @param request 站点、产品和规则
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "绑定站点抽检规则")
    public Result<Boolean> bind(@Valid @RequestBody StepSamplingBindRequest request) {
        service.bind(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出全部绑定。
     *
     * @return 按站点排列
     */
    @GetMapping
    public Result<List<StepSampling>> list() {
        return Result.success(service.list());
    }
}
