package com.scaffold.mes.warehouse.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;

import lombok.RequiredArgsConstructor;

/**
 * MaterialLotQueryApplicationService 物料批查询
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MaterialLotQueryApplicationService {

    private final ReceiptStore store;

    /**
     * 列出物料批。
     *
     * @return 按批号排列
     */
    public List<MaterialLot> list() {
        return store.listLots();
    }
}
