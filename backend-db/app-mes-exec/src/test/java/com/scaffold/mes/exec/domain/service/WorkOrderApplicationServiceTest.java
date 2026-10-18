package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.WorkOrderSaveRequest;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ActiveProcessProvider;
import com.scaffold.share.integration.mes.base.response.FrozenProcess;

/**
 * 创建工单时固化当时的 Route 和 BOM 版本。只有已创建的工单可以下达。
 */
class WorkOrderApplicationServiceTest {

    private WorkOrderRepository repository;
    private WoMaterialRequirementApplicationService requirements;
    private WorkOrderApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(WorkOrderRepository.class);
        ActiveProcessProvider processProvider = mock(ActiveProcessProvider.class);
        when(processProvider.freeze(11L)).thenReturn(new FrozenProcess(1L, 11L, 21L, 31L, 8L));
        requirements = mock(WoMaterialRequirementApplicationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"), ZoneOffset.ofHours(8));
        service = new WorkOrderApplicationService(repository, processProvider, requirements, clock);
    }

    @Test
    void createFreezesActiveRouteAndBom() {
        service.create(request());

        verify(repository).save(org.mockito.ArgumentMatchers.<WorkOrder>argThat(order -> order.getRouteVersionId().equals(21L)
                && order.getBomVersionId().equals(31L)
                && order.getProductId().equals(1L)
                && "CREATED".equals(order.getStatus())
                && order.getLotCount() == 0
                && order.getCustomerId().equals(8L)));
    }

    @Test
    void createRejectsDuplicateNo() {
        when(repository.findByNo("WO-1")).thenReturn(Optional.of(new WorkOrder()));

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WORK_ORDER_NO_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void releaseSetsReleasedAt() {
        WorkOrder order = created();
        when(repository.findById(4L)).thenReturn(Optional.of(order));

        service.release(4L);

        assertThat(order.getStatus()).isEqualTo("RELEASED");
        assertThat(order.getReleasedAt()).isEqualTo(LocalDateTime.of(2026, 10, 16, 12, 0));
        verify(requirements).expand(order);
    }

    @Test
    void releaseRejectsCancelled() {
        WorkOrder order = created();
        order.setStatus("CANCELLED");
        when(repository.findById(4L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.release(4L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WORK_ORDER_STATUS_INVALID);
    }

    private static WorkOrder created() {
        WorkOrder order = new WorkOrder();
        order.setId(4L);
        order.setStatus("CREATED");
        order.setLotCount(0);
        return order;
    }

    private static WorkOrderSaveRequest request() {
        WorkOrderSaveRequest request = new WorkOrderSaveRequest();
        request.setWoNo("WO-1");
        request.setProductVersionId(11L);
        request.setPlanQty(25L);
        request.setQtyUnitId(3L);
        return request;
    }
}
