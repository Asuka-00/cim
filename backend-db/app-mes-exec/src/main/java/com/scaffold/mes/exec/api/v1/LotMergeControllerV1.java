package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotMergeRequest;
import com.scaffold.mes.exec.domain.service.LotMergeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotMergeControllerV1 合批
 * <p>
 * 源 Lot 并入目标 Lot。两边都要未 Hold，并且站点经过次数一致。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/merge")
@RequiredArgsConstructor
public class LotMergeControllerV1 {

    private final LotMergeApplicationService service;

    /**
     * 合批。
     *
     * @param lotId 目标 Lot
     * @param request 源 Lot
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "合批")
    public Result<Boolean> merge(@PathVariable Long lotId, @Valid @RequestBody LotMergeRequest request) {
        service.merge(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
