package com.scaffold.mes.base.api.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.base.domain.dto.StepInstructionResponse;
import com.scaffold.mes.base.domain.dto.StepInstructionSaveRequest;
import com.scaffold.mes.base.domain.service.StepInstructionApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * StepInstructionControllerV1 站点作业指导
 * <p>
 * 给站点挂一段文字指导。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/step-instructions")
@RequiredArgsConstructor
public class StepInstructionControllerV1 {

    private final StepInstructionApplicationService service;

    /**
     * 保存指导。
     *
     * @param request 站点、产品和正文
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "保存站点作业指导")
    public Result<Boolean> save(@Valid @RequestBody StepInstructionSaveRequest request) {
        service.save(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 取出指导。产品专用优先于通用。
     *
     * @param stepId 站点
     * @param productId 产品。空按通用
     * @return 指导。没有时为空
     */
    @GetMapping
    public Result<StepInstructionResponse> show(@RequestParam Long stepId, @RequestParam(required = false) Long productId) {
        return Result.success(service.show(stepId, productId).orElse(null));
    }
}
