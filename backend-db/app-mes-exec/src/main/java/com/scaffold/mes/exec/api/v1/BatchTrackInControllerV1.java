package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.BatchTrackInRequest;
import com.scaffold.mes.exec.domain.service.BatchTrackInApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * BatchTrackInControllerV1 批次进站
 * <p>
 * READY 批次的全部成员在同一事务里进站。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/batches/{batchId}/track-in")
@RequiredArgsConstructor
public class BatchTrackInControllerV1 {

    private final BatchTrackInApplicationService service;

    /**
     * 批次进站。
     *
     * @param batchId 批次
     * @param request 设备和 Recipe
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "批次进站")
    public Result<Boolean> trackIn(@PathVariable Long batchId, @Valid @RequestBody BatchTrackInRequest request) {
        service.trackIn(batchId, request);
        return Result.success(Boolean.TRUE);
    }
}
