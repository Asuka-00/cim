package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.TrackInRequest;
import com.scaffold.mes.exec.domain.service.TrackInApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * TrackInControllerV1 进站
 * <p>
 * Interlock 通过后 Lot 变为 RUNNING。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/track-in")
@RequiredArgsConstructor
public class TrackInControllerV1 {

    private final TrackInApplicationService service;

    /**
     * 进站。
     *
     * @param lotId Lot
     * @param request 站点、设备和 Recipe
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "Track-In")
    public Result<Boolean> trackIn(@PathVariable Long lotId, @Valid @RequestBody TrackInRequest request) {
        service.trackIn(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
