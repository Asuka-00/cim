package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotSkipRequest;
import com.scaffold.mes.exec.domain.service.LotSkipApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotSkipControllerV1 跳站
 * <p>
 * 等待中的 Lot 跳到下一个序号。被跳过的站点不记经过次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/skip")
@RequiredArgsConstructor
public class LotSkipControllerV1 {

    private final LotSkipApplicationService service;

    /**
     * 跳到下一步。
     *
     * @param lotId Lot
     * @param request 原因码
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "跳站")
    public Result<Boolean> skip(@PathVariable Long lotId, @Valid @RequestBody LotSkipRequest request) {
        service.skip(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
