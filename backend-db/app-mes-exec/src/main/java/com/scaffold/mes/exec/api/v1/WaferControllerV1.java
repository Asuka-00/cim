package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.WaferBindRequest;
import com.scaffold.mes.exec.domain.service.WaferApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * WaferControllerV1 绑定 Wafer
 * <p>
 * 给已开批的 Lot 绑定片。片数必须等于 Lot 数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/wafers")
@RequiredArgsConstructor
public class WaferControllerV1 {

    private final WaferApplicationService service;

    /**
     * 绑定 Wafer。
     *
     * @param lotId Lot
     * @param request 指定编号或自动生成片数
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "绑定 Wafer")
    public Result<Boolean> bind(@PathVariable Long lotId, @RequestBody WaferBindRequest request) {
        service.bind(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
