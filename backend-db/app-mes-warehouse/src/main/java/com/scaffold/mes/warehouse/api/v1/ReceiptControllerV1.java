package com.scaffold.mes.warehouse.api.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.dto.ReceiptRequest;
import com.scaffold.mes.warehouse.domain.service.ReceiptApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * ReceiptControllerV1 来料收货
 * <p>
 * 一次确认生成物料批并入库。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/receipts")
@RequiredArgsConstructor
public class ReceiptControllerV1 {

    private final ReceiptApplicationService service;

    /**
     * 确认收货。
     *
     * @param request 单号、批号、数量和库位
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "来料收货")
    public Result<Boolean> receive(@Valid @RequestBody ReceiptRequest request) {
        service.receive(request);
        return Result.success(Boolean.TRUE);
    }
}
