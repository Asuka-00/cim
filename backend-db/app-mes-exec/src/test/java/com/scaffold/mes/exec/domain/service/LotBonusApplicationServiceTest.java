package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotBonusRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.LotRelationRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Bonus 增加开批数量。有工单时先锁工单，合计不能超过计划量。
 */
class LotBonusApplicationServiceTest {

    private LotRepository lotRepository;
    private WorkOrderRepository workOrderRepository;
    private WaferRepository waferRepository;
    private MesTransactionService transactions;
    private Lot lot;
    private LotBonusApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        workOrderRepository = mock(WorkOrderRepository.class);
        waferRepository = mock(WaferRepository.class);
        transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setLotNo("L-1");
        lot.setStatus("WAITING");
        lot.setWoId(4L);
        lot.setQty(2L);
        lot.setInitialQty(2L);
        lot.setWaferCount(2);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(lotRepository.sumInitialQty(4L)).thenReturn(8L);
        WorkOrder order = new WorkOrder();
        order.setPlanQty(10L);
        when(workOrderRepository.findById(4L)).thenReturn(Optional.of(order));
        Wafer existing = new Wafer();
        existing.setSlotNo(2);
        when(waferRepository.listByLot(8L)).thenReturn(List.of(existing));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotBonusApplicationService(transactions, lotRepository, workOrderRepository, waferRepository,
                mock(LotRelationRepository.class), mock(WaferHistoryApplicationService.class),
                mock(LotHistoryApplicationService.class), clock);
    }

    @Test
    void bonusAddsWaferAndLocksWorkOrder() {
        LotBonusRequest request = new LotBonusRequest();
        request.setQty(1L);

        service.bonus(8L, request);

        assertThat(lot.getQty()).isEqualTo(3L);
        assertThat(lot.getInitialQty()).isEqualTo(3L);
        assertThat(lot.getWaferCount()).isEqualTo(3);
        verify(workOrderRepository).lock(4L);
        verify(workOrderRepository, never()).update(any());
        verify(waferRepository).save(org.mockito.ArgumentMatchers.<Wafer>argThat(row -> "L-1.B3".equals(row.getWaferCode())));
        org.mockito.ArgumentCaptor<MesTxnCommand> command = org.mockito.ArgumentCaptor.forClass(MesTxnCommand.class);
        verify(transactions).execute(command.capture(), any(), eq(Boolean.class));
        assertThat(command.getValue().getLocks()).extracting(MesLockTarget::kind).containsExactly("WORK_ORDER", "LOT");
    }

    @Test
    void overPlanRejected() {
        LotBonusRequest request = new LotBonusRequest();
        request.setQty(3L);

        assertThatThrownBy(() -> service.bonus(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_BONUS_OVER_PLAN);
        verify(lotRepository, never()).update(any());
    }
}
