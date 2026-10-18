package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotHoldRequest;
import com.scaffold.mes.exec.domain.dto.LotReleaseRequest;
import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.mes.exec.domain.service.LotHoldApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotHoldControllerV1 Hold 与 Release
 * <p>
 * 手工 Hold 可以叠加。Release 只释放请求里指定的一条。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}")
@RequiredArgsConstructor
public class LotHoldControllerV1 {

    private final LotHoldApplicationService service;

    /**
     * 列出尚未释放的 Hold。
     *
     * @param lotId Lot
     * @return 生效中的 Hold
     */
    @GetMapping("/holds")
    public Result<List<LotHold>> holds(@PathVariable Long lotId) {
        return Result.success(service.listActive(lotId));
    }

    /**
     * 手工 Hold。
     *
     * @param lotId Lot
     * @param request 原因码和备注
     * @return 固定为 true
     */
    @PostMapping("/hold")
    @AuditLog(operation = "Hold Lot")
    public Result<Boolean> hold(@PathVariable Long lotId, @Valid @RequestBody LotHoldRequest request) {
        service.hold(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 释放一条 Hold。
     *
     * @param lotId Lot
     * @param request Hold 记录和备注
     * @return 固定为 true
     */
    @PostMapping("/release")
    @AuditLog(operation = "Release Lot")
    public Result<Boolean> release(@PathVariable Long lotId, @Valid @RequestBody LotReleaseRequest request) {
        service.release(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
