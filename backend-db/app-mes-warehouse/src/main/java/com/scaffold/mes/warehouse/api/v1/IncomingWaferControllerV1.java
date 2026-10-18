package com.scaffold.mes.warehouse.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.dto.IncomingWaferLine;
import com.scaffold.mes.warehouse.domain.entity.IncomingWafer;
import com.scaffold.mes.warehouse.domain.service.IncomingWaferApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * IncomingWaferControllerV1 来料 Wafer 清单
 * <p>
 * 收货后把客户片号挂到物料批上。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/material-lots/{materialLotId}/incoming-wafers")
@RequiredArgsConstructor
public class IncomingWaferControllerV1 {

    private final IncomingWaferApplicationService service;

    /**
     * 录入清单。
     *
     * @param materialLotId 物料批
     * @param lines 来料片
     * @return 固定为 true
     */
    @PostMapping
    @AuditLog(operation = "录入来料 Wafer")
    public Result<Boolean> record(@PathVariable Long materialLotId,
            @Valid @RequestBody List<@Valid IncomingWaferLine> lines) {
        service.record(materialLotId, lines);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 列出来料片。
     *
     * @param materialLotId 物料批
     * @return 来料片
     */
    @GetMapping
    public Result<List<IncomingWafer>> list(@PathVariable Long materialLotId) {
        return Result.success(service.list(materialLotId));
    }
}
