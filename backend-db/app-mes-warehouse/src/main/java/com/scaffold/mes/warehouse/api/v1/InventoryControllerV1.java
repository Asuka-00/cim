package com.scaffold.mes.warehouse.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.service.InventoryQueryApplicationService;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * InventoryControllerV1 库存查询
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/inventories")
@RequiredArgsConstructor
public class InventoryControllerV1 {

    private final InventoryQueryApplicationService service;

    /**
     * 列出库存。
     *
     * @return 全部库存行
     */
    @GetMapping
    public Result<List<Inventory>> list() {
        return Result.success(service.list());
    }
}
