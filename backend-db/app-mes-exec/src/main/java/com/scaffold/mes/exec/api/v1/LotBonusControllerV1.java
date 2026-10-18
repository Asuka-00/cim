package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotBonusRequest;
import com.scaffold.mes.exec.domain.service.LotBonusApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotBonusControllerV1 补片
 * <p>
 * 给在制 Lot 增加开批数量。有工单时不能超过计划量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/bonus")
@RequiredArgsConstructor
public class LotBonusControllerV1 {

    private final LotBonusApplicationService service;

    /**
     * 补片。
     *
     * @param lotId Lot
     * @param request 补入数量
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "Bonus")
    public Result<Boolean> bonus(@PathVariable Long lotId, @Valid @RequestBody LotBonusRequest request) {
        service.bonus(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
