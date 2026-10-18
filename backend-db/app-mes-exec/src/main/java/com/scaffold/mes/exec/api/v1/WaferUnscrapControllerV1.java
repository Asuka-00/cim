package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.service.WaferUnscrapApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * WaferUnscrapControllerV1 取消报废
 * <p>
 * 报废片恢复到报废前的状态，数量加回所属 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/wafers/{waferId}/unscrap")
@RequiredArgsConstructor
public class WaferUnscrapControllerV1 {

    private final WaferUnscrapApplicationService service;

    /**
     * 取消报废。
     *
     * @param waferId Wafer
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "取消报废")
    public Result<Boolean> unscrap(@PathVariable Long waferId) {
        service.unscrap(waferId);
        return Result.success(Boolean.TRUE);
    }
}
