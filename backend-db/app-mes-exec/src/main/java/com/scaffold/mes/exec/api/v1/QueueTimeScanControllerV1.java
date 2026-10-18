package com.scaffold.mes.exec.api.v1;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.service.QueueTimeScanApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * QueueTimeScanControllerV1 排队时间扫描
 * <p>
 * 扣押还没进站、等待已超过步骤上限的 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/queue-times/scan")
@RequiredArgsConstructor
public class QueueTimeScanControllerV1 {

    private final QueueTimeScanApplicationService service;

    /**
     * 扫描超时等待。
     *
     * @param now 当前时间，空则取系统时间
     * @return 新扣押的批数
     */
    @PostMapping
    @AuditLog(operation = "扫描排队时间")
    public Result<Integer> scan(@RequestParam(required = false) LocalDateTime now) {
        return Result.success(service.scan(now == null ? LocalDateTime.now() : now));
    }
}
