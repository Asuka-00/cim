package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotCancelTrackInRequest;
import com.scaffold.mes.exec.domain.service.BatchEjectApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * BatchEjectControllerV1 加工中剔除
 * <p>
 * RUNNING 批次里的一个 Lot 取消进站并离开批次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/batches/{batchId}/lots/{lotId}/eject")
@RequiredArgsConstructor
public class BatchEjectControllerV1 {

    private final BatchEjectApplicationService service;

    /**
     * 剔除 Lot。
     *
     * @param batchId 批次
     * @param lotId Lot
     * @param request 原因
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "剔除批次")
    public Result<Boolean> eject(@PathVariable Long batchId, @PathVariable Long lotId,
            @Valid @RequestBody LotCancelTrackInRequest request) {
        service.eject(batchId, lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
