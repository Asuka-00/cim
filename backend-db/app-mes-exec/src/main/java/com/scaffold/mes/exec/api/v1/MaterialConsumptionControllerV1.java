package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.ConsumptionAdjustRequest;
import com.scaffold.mes.exec.domain.service.MaterialConsumptionApplicationService;

import jakarta.validation.Valid;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MaterialConsumptionControllerV1 欠账清偿
 * <p>
 * 线边够扣时补扣，并写下冲销和实际消耗。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/consumptions")
@RequiredArgsConstructor
public class MaterialConsumptionControllerV1 {

    private final MaterialConsumptionApplicationService service;

    /**
     * 清偿一笔欠账。
     *
     * @param id 欠账消耗
     * @return 固定为 true
     */
    @PostMapping("/{id}/settle")
    @AuditLog(operation = "清偿欠账")
    public Result<Boolean> settle(@PathVariable Long id) {
        service.settle(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 修正消耗数量。
     *
     * @param id 原消耗
     * @param request 修正后的数量
     * @return 固定为 true
     */
    @PostMapping("/{id}/adjust")
    @AuditLog(operation = "修正消耗")
    public Result<Boolean> adjust(@PathVariable Long id, @Valid @RequestBody ConsumptionAdjustRequest request) {
        service.adjust(id, request.getQtyAfter());
        return Result.success(Boolean.TRUE);
    }
}
