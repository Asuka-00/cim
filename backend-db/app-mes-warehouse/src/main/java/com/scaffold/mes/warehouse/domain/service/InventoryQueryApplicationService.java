package com.scaffold.mes.warehouse.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;

import lombok.RequiredArgsConstructor;

/**
 * InventoryQueryApplicationService 库存查询
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class InventoryQueryApplicationService {

    private final ReceiptStore store;

    /**
     * 列出库存。
     *
     * @return 全部库存行
     */
    public List<Inventory> list() {
        return store.listInventories();
    }
}
