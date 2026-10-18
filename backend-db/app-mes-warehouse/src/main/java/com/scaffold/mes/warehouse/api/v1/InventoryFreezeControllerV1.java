package com.scaffold.mes.warehouse.api.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.dto.InventoryFreezeRequest;
import com.scaffold.mes.warehouse.domain.service.InventoryFreezeApplicationService;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * InventoryFreezeControllerV1 库位部分冻结
 * <p>
 * 增加该库位的冻结数量。可用量随之减少。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/inventories")
@RequiredArgsConstructor
public class InventoryFreezeControllerV1 {

    private final InventoryFreezeApplicationService service;

    /**
     * 冻结一部分数量。
     *
     * @param request 物料批、库位和数量
     * @return 固定为 true
     */
    @PostMapping("/freeze")
    @AuditLog(operation = "库位冻结")
    public Result<Boolean> freeze(@Valid @RequestBody InventoryFreezeRequest request) {
        service.freeze(request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 解冻一部分数量。
     *
     * @param request 物料批、库位和数量
     * @return 固定为 true
     */
    @PostMapping("/unfreeze")
    @AuditLog(operation = "库位解冻")
    public Result<Boolean> unfreeze(@Valid @RequestBody InventoryFreezeRequest request) {
        service.unfreeze(request);
        return Result.success(Boolean.TRUE);
    }
}
