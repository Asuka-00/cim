package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotForceMoveRequest;
import com.scaffold.mes.exec.domain.service.LotForceMoveApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotForceMoveControllerV1 强制跳站
 * <p>
 * 跳到当前 Route 上的任意步骤。被跳过的站点不记经过次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/force-move")
@RequiredArgsConstructor
public class LotForceMoveControllerV1 {

    private final LotForceMoveApplicationService service;

    /**
     * 强制跳站。
     *
     * @param lotId Lot
     * @param request 目标序号和原因
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "强制跳站")
    public Result<Boolean> move(@PathVariable Long lotId, @Valid @RequestBody LotForceMoveRequest request) {
        service.move(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
