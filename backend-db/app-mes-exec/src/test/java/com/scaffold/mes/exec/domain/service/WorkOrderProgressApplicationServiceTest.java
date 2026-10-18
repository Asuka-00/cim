package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;

/**
 * CONSUMED 计入完成数量。TERMINATED 只计入终止。全部终态且数量够计划时工单完成。
 */
class WorkOrderProgressApplicationServiceTest {

    private WorkOrder order;
    private WorkOrderProgressApplicationService service;

    @BeforeEach
    void setUp() {
        WorkOrderRepository orders = mock(WorkOrderRepository.class);
        LotRepository lots = mock(LotRepository.class);
        order = new WorkOrder();
        order.setId(3L);
        order.setStatus("RELEASED");
        order.setPlanQty(10L);
        when(orders.findById(3L)).thenReturn(Optional.of(order));
        when(lots.listByWorkOrder(3L)).thenReturn(List.of(
                lot("CONSUMED", 8L, 2L),
                lot("TERMINATED", 4L, 0L),
                lot("MERGED", 0L, 0L)));
        service = new WorkOrderProgressApplicationService(orders, lots);
    }

    @Test
    void consumedCountsAsCompleted() {
        service.refresh(3L);

        assertThat(order.getCompletedQty()).isEqualTo(6L);
        assertThat(order.getTerminatedQty()).isEqualTo(4L);
        assertThat(order.getScrapQty()).isEqualTo(2L);
        assertThat(order.getStartedQty()).isEqualTo(12L);
        assertThat(order.getStatus()).isEqualTo("COMPLETED");
    }

    private static Lot lot(String status, long initial, long scrap) {
        Lot lot = new Lot();
        lot.setStatus(status);
        lot.setInitialQty(initial);
        lot.setScrapQty(scrap);
        return lot;
    }
}
