package com.scaffold.mes.exec.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotSplitRequest;
import com.scaffold.mes.exec.domain.service.LotSplitApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotSplitControllerV1 拆批
 * <p>
 * 从等待中的 Lot 拆出子 Lot。父 Lot 至少留下一片。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/split")
@RequiredArgsConstructor
public class LotSplitControllerV1 {

    private final LotSplitApplicationService service;

    /**
     * 拆批。
     *
     * @param lotId 父 Lot
     * @param request 要拆走的片
     * @return 子 Lot 号
     */
    @PostMapping
    @AuditLog(operation = "拆批")
    public Result<String> split(@PathVariable Long lotId, @Valid @RequestBody LotSplitRequest request) {
        return Result.success(service.split(lotId, request));
    }
}
