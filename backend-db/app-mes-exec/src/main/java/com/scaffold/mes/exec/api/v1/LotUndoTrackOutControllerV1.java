package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotUndoTrackOutRequest;
import com.scaffold.mes.exec.domain.service.LotUndoTrackOutApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotUndoTrackOutControllerV1 撤销出站
 * <p>
 * 按最近一次出站前的快照回到加工中。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/undo-track-out")
@RequiredArgsConstructor
public class LotUndoTrackOutControllerV1 {

    private final LotUndoTrackOutApplicationService service;

    /**
     * 撤销出站。
     *
     * @param lotId Lot
     * @param request 原因码
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "撤销出站")
    public Result<Boolean> undo(@PathVariable Long lotId, @Valid @RequestBody LotUndoTrackOutRequest request) {
        service.undo(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
