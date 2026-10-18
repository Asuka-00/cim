package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotReworkRequest;
import com.scaffold.mes.exec.domain.service.LotReworkApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotReworkControllerV1 进入返工
 * <p>
 * 当前步骤必须落在分支允许的范围内，并且根 Lot 还没超过次数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/rework")
@RequiredArgsConstructor
public class LotReworkControllerV1 {

    private final LotReworkApplicationService service;

    /**
     * 进入返工。
     *
     * @param lotId Lot
     * @param request 返工分支
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "进入返工")
    public Result<Boolean> enter(@PathVariable Long lotId, @Valid @RequestBody LotReworkRequest request) {
        service.enter(lotId, request);
        return Result.success(Boolean.TRUE);
    }
}
