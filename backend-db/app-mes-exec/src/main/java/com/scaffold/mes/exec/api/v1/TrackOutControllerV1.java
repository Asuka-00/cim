package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.TrackOutRequest;
import com.scaffold.mes.exec.domain.service.TrackOutApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * TrackOutControllerV1 出站
 * <p>
 * 加工中的 Lot 进入下一步，或在最后一步完成。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/track-out")
@RequiredArgsConstructor
public class TrackOutControllerV1 {

    private final TrackOutApplicationService service;

    /**
     * 出站。
     *
     * @param lotId Lot
     * @param request 备注，可以不传
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "Track-Out")
    public Result<Boolean> trackOut(@PathVariable Long lotId, @RequestBody(required = false) TrackOutRequest request) {
        service.trackOut(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
