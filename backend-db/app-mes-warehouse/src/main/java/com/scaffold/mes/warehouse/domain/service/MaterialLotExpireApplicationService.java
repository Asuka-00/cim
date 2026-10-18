package com.scaffold.mes.warehouse.domain.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;

import lombok.RequiredArgsConstructor;

/**
 * MaterialLotExpireApplicationService 物料批过期
 * <p>
 * 到期日早于今天的可用或待检批改为 EXPIRED。过期后不能领料。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MaterialLotExpireApplicationService {

    private final ReceiptStore store;

    /**
     * 把已到期的物料批标成过期。
     *
     * @param today 今天
     * @return 改了几批
     */
    @Transactional
    public int expireDue(LocalDate today) {
        int count = 0;
        for (MaterialLot lot : store.listDue(today)) {
            lot.setStatus("EXPIRED");
            store.updateLot(lot);
            count++;
        }
        return count;
    }
}
