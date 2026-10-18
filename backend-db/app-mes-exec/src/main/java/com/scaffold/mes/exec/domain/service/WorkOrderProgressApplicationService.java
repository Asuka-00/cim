package com.scaffold.mes.exec.domain.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;

import lombok.RequiredArgsConstructor;

/**
 * WorkOrderProgressApplicationService 工单进度重算
 * <p>
 * 完成态 COMPLETED、SHIPPED、CONSUMED 把 initial_qty 减去 scrap_qty 计入完成。TERMINATED 计入终止，不算完成。MERGED 不计入。报废按各 Lot 的 scrap_qty 相加。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WorkOrderProgressApplicationService {

    private static final Set<String> DONE = Set.of("COMPLETED", "SHIPPED", "CONSUMED");
    private static final Set<String> TERMINAL = Set.of("COMPLETED", "SHIPPED", "CONSUMED", "SCRAPPED", "TERMINATED", "MERGED");

    private final WorkOrderRepository workOrderRepository;
    private final LotRepository lotRepository;

    /**
     * 按当前 Lot 重算工单进度。工单不存在时直接返回。
     *
     * @param woId 工单
     */
    @Transactional
    public void refresh(Long woId) {
        WorkOrder order = workOrderRepository.findById(woId).orElse(null);
        if (order == null) {
            return;
        }
        long completed = 0L;
        long scrap = 0L;
        long terminated = 0L;
        long started = 0L;
        int lotCount = 0;
        boolean allTerminal = true;
        var lots = lotRepository.listByWorkOrder(woId);
        if (lots.isEmpty()) {
            allTerminal = false;
        }
        for (Lot lot : lots) {
            long initial = lot.getInitialQty() == null ? 0L : lot.getInitialQty();
            long scrapped = lot.getScrapQty() == null ? 0L : lot.getScrapQty();
            long remain = Math.max(0L, initial - scrapped);
            started += initial;
            scrap += scrapped;
            String status = lot.getStatus();
            if ("MERGED".equals(status)) {
                continue;
            }
            lotCount++;
            if (DONE.contains(status)) {
                completed += remain;
            } else if ("TERMINATED".equals(status)) {
                terminated += remain;
            }
            if (!TERMINAL.contains(status)) {
                allTerminal = false;
            }
        }
        order.setStartedQty(started);
        order.setCompletedQty(completed);
        order.setScrapQty(scrap);
        order.setTerminatedQty(terminated);
        order.setLotCount(lotCount);
        long plan = order.getPlanQty() == null ? 0L : order.getPlanQty();
        if (allTerminal && "RELEASED".equals(order.getStatus()) && completed + scrap + terminated >= plan) {
            order.setStatus("COMPLETED");
        }
        workOrderRepository.update(order);
    }
}
