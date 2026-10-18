package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotPriorityRequest;
import com.scaffold.mes.exec.domain.service.LotPriorityApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotPriorityControllerV1 修改优先级
 * <p>
 * Hold 中的在制 Lot 也可以改。取值 1 到 9。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/priority")
@RequiredArgsConstructor
public class LotPriorityControllerV1 {

    private final LotPriorityApplicationService service;

    /**
     * 修改优先级。
     *
     * @param lotId Lot
     * @param request 新优先级
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "修改 Lot 优先级")
    public Result<Boolean> change(@PathVariable Long lotId, @Valid @RequestBody LotPriorityRequest request) {
        service.change(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
