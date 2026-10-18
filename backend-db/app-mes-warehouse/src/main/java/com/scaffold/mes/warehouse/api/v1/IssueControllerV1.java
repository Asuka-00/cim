package com.scaffold.mes.warehouse.api.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.dto.IssueRequest;
import com.scaffold.mes.warehouse.domain.service.IssueApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * IssueControllerV1 领料
 * <p>
 * 只有可用物料批可以从源库位领到目标库位。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes")
@RequiredArgsConstructor
public class IssueControllerV1 {

    private final IssueApplicationService service;

    /**
     * 领料。
     *
     * @param request 物料批、库位和数量
     * @return 固定为 true
     */
    @PostMapping("/issues")
    @AuditLog(operation = "领料")
    public Result<Boolean> issue(@Valid @RequestBody IssueRequest request) {
        service.issue(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 退料。
     *
     * @param request from 是当前库位，to 是退回库位
     * @return 固定为 true
     */
    @PostMapping("/returns")
    @AuditLog(operation = "退料")
    public Result<Boolean> returnMaterial(@Valid @RequestBody IssueRequest request) {
        service.returnMaterial(request);
        return Result.success(Boolean.TRUE);
    }
}
