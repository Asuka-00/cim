package com.scaffold.mes.exec.api.v1;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.service.LotTimeLinkTimerApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * TimeLinkScanControllerV1 时间链超时扫描
 * <p>
 * 处理已过截止时间的计时。HOLD 会扣押，WARN 只记预警。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/time-link-timers/scan")
@RequiredArgsConstructor
public class TimeLinkScanControllerV1 {

    private final LotTimeLinkTimerApplicationService service;

    /**
     * 扫描超时计时。
     *
     * @param now 当前时间，空则取系统时间
     * @return 处理了几条
     */
    @PostMapping
    @AuditLog(operation = "扫描时间链")
    public Result<Integer> scan(@RequestParam(required = false) LocalDateTime now) {
        return Result.success(service.scan(now == null ? LocalDateTime.now() : now));
    }
}
