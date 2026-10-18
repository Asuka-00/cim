package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotRouteChangeRequest;
import com.scaffold.mes.exec.domain.service.LotRouteChangeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotRouteChangeControllerV1 改 Route
 * <p>
 * 等待中的 Lot 改到新 Route 的指定步骤。旧版本上未执行的预约会被取消。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/route")
@RequiredArgsConstructor
public class LotRouteChangeControllerV1 {

    private final LotRouteChangeApplicationService service;

    /**
     * 改 Route。
     *
     * @param lotId Lot
     * @param request 新版本、步骤和原因
     * @return 被取消的旧预约条数
     */
    @PostMapping
    @AuditLog(operation = "改 Route")
    public Result<Integer> change(@PathVariable Long lotId, @Valid @RequestBody LotRouteChangeRequest request) {
        return Result.success(service.change(lotId, request));
    }
}
