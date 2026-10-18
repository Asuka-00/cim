package com.scaffold.mes.warehouse.api.v1;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.service.MaterialLotFreezeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MaterialLotFreezeControllerV1 整批冻结
 * <p>
 * 冻结后的物料批不能领料。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/material-lots/{materialLotId}")
@RequiredArgsConstructor
public class MaterialLotFreezeControllerV1 {

    private final MaterialLotFreezeApplicationService service;

    /**
     * 整批冻结。
     *
     * @param materialLotId 物料批
     * @return 固定为 true
     */
    @PostMapping("/freeze")
    @AuditLog(operation = "整批冻结")
    public Result<Boolean> freeze(@PathVariable Long materialLotId) {
        service.freeze(materialLotId);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 解冻。
     *
     * @param materialLotId 物料批
     * @return 固定为 true
     */
    @PostMapping("/unfreeze")
    @AuditLog(operation = "整批解冻")
    public Result<Boolean> unfreeze(@PathVariable Long materialLotId) {
        service.unfreeze(materialLotId);
        return Result.success(Boolean.TRUE);
    }
}
