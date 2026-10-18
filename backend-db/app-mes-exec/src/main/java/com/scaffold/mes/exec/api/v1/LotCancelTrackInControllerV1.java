package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotCancelTrackInRequest;
import com.scaffold.mes.exec.domain.service.LotCancelTrackInApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotCancelTrackInControllerV1 取消进站
 * <p>
 * 加工中的 Lot 回到等待，并离开设备。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/cancel-track-in")
@RequiredArgsConstructor
public class LotCancelTrackInControllerV1 {

    private final LotCancelTrackInApplicationService service;

    /**
     * 取消进站。
     *
     * @param lotId Lot
     * @param request 原因码
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "取消进站")
    public Result<Boolean> cancel(@PathVariable Long lotId, @Valid @RequestBody LotCancelTrackInRequest request) {
        service.cancel(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
