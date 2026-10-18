package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotPrintRequest;
import com.scaffold.mes.exec.domain.dto.LotPrintResponse;
import com.scaffold.mes.exec.domain.service.LotPrintApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotPrintControllerV1 记录并列出 Lot 打印
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{id}/prints")
@RequiredArgsConstructor
public class LotPrintControllerV1 {

    private final LotPrintApplicationService service;

    /**
     * 用已有模板记一条打印。
     *
     * @param id Lot
     * @param request 模板编码和份数
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "打印 Lot")
    public Result<Boolean> record(@PathVariable Long id, @Valid @RequestBody LotPrintRequest request) {
        service.record(id, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出这个 Lot 的打印记录。
     *
     * @param id Lot
     * @return 按打印时间从早到晚
     */
    @GetMapping
    public Result<List<LotPrintResponse>> list(@PathVariable Long id) {
        return Result.success(service.list(id));
    }
}
