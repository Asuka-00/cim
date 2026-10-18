package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.LotNoteRequest;
import com.scaffold.mes.exec.domain.entity.LotNote;
import com.scaffold.mes.exec.domain.service.LotNoteApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * LotNoteControllerV1 Lot 备注
 * <p>
 * 追加和查看备注。Hold 中也可以写。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/notes")
@RequiredArgsConstructor
public class LotNoteControllerV1 {

    private final LotNoteApplicationService service;

    /**
     * 追加备注。
     *
     * @param lotId Lot
     * @param request 类型和内容
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "追加 Lot 备注")
    public Result<Boolean> add(@PathVariable Long lotId, @Valid @RequestBody LotNoteRequest request) {
        service.add(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 查看备注。
     *
     * @param lotId Lot
     * @return 置顶在前的备注
     */
    @GetMapping
    public Result<List<LotNote>> list(@PathVariable Long lotId) {
        return Result.success(service.list(lotId));
    }
}
