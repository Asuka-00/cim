package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotScrapRequest;
import com.scaffold.mes.exec.domain.dto.WaferScrapRequest;
import com.scaffold.mes.exec.domain.service.ScrapApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ScrapControllerV1 报废
 * <p>
 * 整批报废和单片报废。原因码必须是启用的 MES_SCRAP_REASON。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes")
@RequiredArgsConstructor
public class ScrapControllerV1 {

    private final ScrapApplicationService service;

    /**
     * 整批报废。
     *
     * @param lotId Lot
     * @param request 原因码和数量
     * @return 固定为 true
     */
    @PostMapping("/lots/{lotId}/scrap")
    @AuditLog(operation = "报废 Lot")
    public Result<Boolean> scrapLot(@PathVariable Long lotId, @Valid @RequestBody LotScrapRequest request) {
        service.scrapLot(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 单片报废。
     *
     * @param waferId Wafer
     * @param request 原因码
     * @return 固定为 true
     */
    @PostMapping("/wafers/{waferId}/scrap")
    @AuditLog(operation = "报废 Wafer")
    public Result<Boolean> scrapWafer(@PathVariable Long waferId, @Valid @RequestBody WaferScrapRequest request) {
        service.scrapWafer(waferId, request);
        return Result.success(Boolean.TRUE);
    }
}
