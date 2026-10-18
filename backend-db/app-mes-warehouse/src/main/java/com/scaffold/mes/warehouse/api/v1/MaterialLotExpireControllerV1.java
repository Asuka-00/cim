package com.scaffold.mes.warehouse.api.v1;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.service.MaterialLotExpireApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MaterialLotExpireControllerV1 物料批过期
 * <p>
 * 把到期日早于今天的可用或待检批改为 EXPIRED。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/material-lots/expire-due")
@RequiredArgsConstructor
public class MaterialLotExpireControllerV1 {

    private final MaterialLotExpireApplicationService service;

    /**
     * 处理到期批。
     *
     * @param today 今天，空则取系统当天
     * @return 改了几批
     */
    @PostMapping
    @AuditLog(operation = "物料批过期")
    public Result<Integer> expire(@RequestParam(required = false) LocalDate today) {
        return Result.success(service.expireDue(today == null ? LocalDate.now() : today));
    }
}
