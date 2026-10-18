package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotMoveBackRequest;
import com.scaffold.mes.exec.domain.service.LotMoveBackApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotMoveBackControllerV1 退回上一步
 * <p>
 * 回到前一个步骤重走。不回减已经记过的经过次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/move-back")
@RequiredArgsConstructor
public class LotMoveBackControllerV1 {

    private final LotMoveBackApplicationService service;

    /**
     * 退回上一步。
     *
     * @param lotId Lot
     * @param request 原因码
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "退回上一步")
    public Result<Boolean> moveBack(@PathVariable Long lotId, @Valid @RequestBody LotMoveBackRequest request) {
        service.moveBack(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
