package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotShipRequest;
import com.scaffold.mes.exec.domain.service.LotShipApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotShipControllerV1 出货
 * <p>
 * 已完成且未 Hold 的 Lot 整批出货。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/ship")
@RequiredArgsConstructor
public class LotShipControllerV1 {

    private final LotShipApplicationService service;

    /**
     * 出货。
     *
     * @param lotId Lot
     * @param request 出货单号
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "出货")
    public Result<Boolean> ship(@PathVariable Long lotId, @Valid @RequestBody LotShipRequest request) {
        service.ship(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
