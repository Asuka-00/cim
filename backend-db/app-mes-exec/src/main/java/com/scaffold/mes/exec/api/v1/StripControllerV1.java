package com.scaffold.mes.exec.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.exec.domain.dto.StripConvertRequest;
import com.scaffold.mes.exec.domain.dto.StripCreateRequest;
import com.scaffold.mes.exec.domain.entity.Strip;
import com.scaffold.mes.exec.domain.service.StripApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * StripControllerV1 建立 Strip
 * <p>
 * 把一条 Strip 挂到在制 Lot 上。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/lots/{lotId}/strips")
@RequiredArgsConstructor
public class StripControllerV1 {

    private final StripApplicationService service;

    /**
     * 建立 Strip。
     *
     * @param lotId Lot
     * @param request 编号和数量
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "建立 Strip")
    public Result<Boolean> create(@PathVariable Long lotId, @Valid @RequestBody StripCreateRequest request) {
        service.create(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 切筋成型。
     *
     * @param lotId Lot
     * @param request Unit 计数单位
     * @return 固定为 true
     */
    @PostMapping("/convert")
    @AuditLog(operation = "切筋成型")
    public Result<Boolean> convert(@PathVariable Long lotId, @Valid @RequestBody StripConvertRequest request) {
        service.convert(lotId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出某个 Lot 的 Strip。
     *
     * @param lotId Lot
     * @return Strip 列表
     */
    @GetMapping
    public Result<List<Strip>> list(@PathVariable Long lotId) {
        return Result.success(service.list(lotId));
    }
}
