package com.scaffold.mes.warehouse.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.dto.InspectionRequest;
import com.scaffold.mes.warehouse.domain.service.IncomingInspectionApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * IncomingInspectionControllerV1 来料检验
 * <p>
 * 合格后物料批变为可用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/material-lots/{materialLotId}/inspections")
@RequiredArgsConstructor
public class IncomingInspectionControllerV1 {

    private final IncomingInspectionApplicationService service;

    /**
     * 提交检验结论。
     *
     * @param materialLotId 物料批
     * @param request 结论
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "来料检验")
    public Result<Boolean> inspect(@PathVariable Long materialLotId, @Valid @RequestBody InspectionRequest request) {
        service.inspect(materialLotId, request);
        return Result.success(Boolean.TRUE);
    }
}
