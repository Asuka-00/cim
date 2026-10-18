package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotStartRequest;
import com.scaffold.mes.exec.domain.dto.WipImportRequest;
import com.scaffold.mes.exec.domain.service.LotApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotControllerV1 开批接口
 * <p>
 * 从已下达工单开批，或导入一条停在指定站点的期初在制。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/work-orders/{woId}/lots")
@RequiredArgsConstructor
public class LotControllerV1 {

    private final LotApplicationService service;

    /**
     * 开批。
     *
     * @param woId 工单
     * @param request Lot 号和数量
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "开批")
    public Result<Boolean> start(@PathVariable Long woId, @Valid @RequestBody LotStartRequest request) {
        service.start(woId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 导入一条期初在制。
     *
     * @param woId 工单
     * @param request 批号、数量和当前站点
     * @return 固定为 true
     */
    @PostMapping("/import")
    @AuditLog(operation = "导入期初在制")
    public Result<Boolean> importWip(@PathVariable Long woId, @Valid @RequestBody WipImportRequest request) {
        service.importWip(woId, request);
        return Result.success(Boolean.TRUE);
    }
}
