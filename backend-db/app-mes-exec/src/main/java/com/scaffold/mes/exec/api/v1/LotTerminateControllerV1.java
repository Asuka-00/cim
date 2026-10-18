package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotTerminateRequest;
import com.scaffold.mes.exec.domain.service.LotTerminateApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotTerminateControllerV1 终止
 * <p>
 * 在制 Lot 变为 TERMINATED。数量保留。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/terminate")
@RequiredArgsConstructor
public class LotTerminateControllerV1 {

    private final LotTerminateApplicationService service;

    /**
     * 终止。
     *
     * @param lotId Lot
     * @param request 原因码
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "终止 Lot")
    public Result<Boolean> terminate(@PathVariable Long lotId, @Valid @RequestBody LotTerminateRequest request) {
        service.terminate(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
