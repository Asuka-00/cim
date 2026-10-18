package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.DieBinLine;
import com.scaffold.mes.exec.domain.entity.LotDieBin;
import com.scaffold.mes.exec.domain.service.DieBinApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * DieBinControllerV1 Die Bin 汇总
 * <p>
 * 没有逐片明细时，替换 Lot 的 Bin 汇总。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/die-bins")
@RequiredArgsConstructor
public class DieBinControllerV1 {

    private final DieBinApplicationService service;

    /**
     * 替换汇总行。
     *
     * @param lotId Lot
     * @param lines Bin 行
     * @return 固定为 true
     */
    @PutMapping
    @AuditLog(operation = "录入 Die Bin")
    public Result<Boolean> replace(@PathVariable Long lotId, @Valid @RequestBody List<@Valid DieBinLine> lines) {
        service.replaceSummary(lotId, lines);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出某个 Lot 的 Bin 行。
     *
     * @param lotId Lot
     * @return 汇总行和逐片行
     */
    @GetMapping
    public Result<List<LotDieBin>> list(@PathVariable Long lotId) {
        return Result.success(service.list(lotId));
    }
}
