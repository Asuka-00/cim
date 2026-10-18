package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.WaferTrackRequest;
import com.scaffold.mes.exec.domain.service.WaferTrackApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * WaferTrackControllerV1 片级进出站
 * <p>
 * 没选中的片留在原步骤。选中的片跟原 Lot 继续。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/wafer-track")
@RequiredArgsConstructor
public class WaferTrackControllerV1 {

    private final WaferTrackApplicationService service;

    /**
     * 片级进站。
     *
     * @param lotId Lot
     * @param request 选中的片和设备
     * @return 固定为 true
     */
    @PostMapping("/in")
    @AuditLog(operation = "片级进站")
    public Result<Boolean> trackIn(@PathVariable Long lotId, @Valid @RequestBody WaferTrackRequest request) {
        service.trackIn(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 片级出站。
     *
     * @param lotId Lot
     * @param request 选中的片
     * @return 固定为 true
     */
    @PostMapping("/out")
    @AuditLog(operation = "片级出站")
    public Result<Boolean> trackOut(@PathVariable Long lotId, @Valid @RequestBody WaferTrackRequest request) {
        service.trackOut(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
