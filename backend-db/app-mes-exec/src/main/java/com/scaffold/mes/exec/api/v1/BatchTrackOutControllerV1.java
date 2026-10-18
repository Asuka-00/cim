package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.TrackOutRequest;
import com.scaffold.mes.exec.domain.service.BatchTrackOutApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * BatchTrackOutControllerV1 批次出站
 * <p>
 * RUNNING 批次的全部成员在同一事务里出站。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/batches/{batchId}/track-out")
@RequiredArgsConstructor
public class BatchTrackOutControllerV1 {

    private final BatchTrackOutApplicationService service;

    /**
     * 批次出站。
     *
     * @param batchId 批次
     * @param request 备注，可空
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "批次出站")
    public Result<Boolean> trackOut(@PathVariable Long batchId,
            @RequestBody(required = false) TrackOutRequest request) {
        service.trackOut(batchId, request);
        return Result.success(Boolean.TRUE);
    }
}
