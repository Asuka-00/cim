package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.PackageInputRequest;
import com.scaffold.mes.exec.domain.service.PackageInputApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * PackageInputControllerV1 封装投入
 * <p>
 * 从源 Lot 扣数量到封装 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/package-input")
@RequiredArgsConstructor
public class PackageInputControllerV1 {

    private final PackageInputApplicationService service;

    /**
     * 投入。
     *
     * @param lotId 封装 Lot
     * @param request 源 Lot 和数量
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "封装投入")
    public Result<Boolean> input(@PathVariable Long lotId, @Valid @RequestBody PackageInputRequest request) {
        service.input(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
