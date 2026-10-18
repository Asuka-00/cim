package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.WaferReenterRequest;
import com.scaffold.mes.exec.domain.service.WaferReclaimApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * WaferReclaimControllerV1 Wafer 回收
 * <p>
 * 报废或终止后的片可以回收。再入厂时使用新的内部编号。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes")
@RequiredArgsConstructor
public class WaferReclaimControllerV1 {

    private final WaferReclaimApplicationService service;

    /**
     * 回收一片。
     *
     * @param waferId Wafer
     * @return 固定为 true
     */
    @PostMapping("/wafers/{waferId}/reclaim")
    @AuditLog(operation = "回收 Wafer")
    public Result<Boolean> reclaim(@PathVariable Long waferId) {
        service.reclaim(waferId);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 回收片再入厂。
     *
     * @param lotId 接收的 Lot
     * @param request 原片和新编号
     * @return 固定为 true
     */
    @PostMapping("/lots/{lotId}/wafers/reenter")
    @AuditLog(operation = "回收片再入厂")
    public Result<Boolean> reenter(@PathVariable Long lotId, @Valid @RequestBody WaferReenterRequest request) {
        service.reenter(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
