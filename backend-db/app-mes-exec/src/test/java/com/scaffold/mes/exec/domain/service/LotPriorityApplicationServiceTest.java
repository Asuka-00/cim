package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.LotPriorityRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Hold 中的在制 Lot 可以改优先级。终态和超出 1 到 9 会被拒绝。
 */
class LotPriorityApplicationServiceTest {

    private LotRepository lotRepository;
    private LotHistoryApplicationService history;
    private MesTransactionService transactions;
    private Lot lot;
    private LotPriorityApplicationService service;

    @BeforeEach
    void setUp() {
        lotRepository = mock(LotRepository.class);
        history = mock(LotHistoryApplicationService.class);
        transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setHoldCount(2);
        lot.setPriority(5);
        lot.setQty(2L);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        service = new LotPriorityApplicationService(transactions, lotRepository, history);
    }

    @Test
    void heldLotCanChangePriority() {
        service.change(8L, request(1));

        assertThat(lot.getPriority()).isEqualTo(1);
        assertThat(lot.getHoldCount()).isEqualTo(2);
        verify(history).record(eq(lot), eq("PRIORITY"), isNull(), isNull(), isNull(), eq("WAITING"), eq(2L));
        ArgumentCaptor<MesTxnCommand> command = ArgumentCaptor.forClass(MesTxnCommand.class);
        verify(transactions).execute(command.capture(), any(), eq(Boolean.class));
        assertThat(command.getValue().getLocks()).extracting(MesLockTarget::kind).containsExactly("LOT");
    }

    @Test
    void outOfRangeRejectedBeforeLock() {
        assertThatThrownBy(() -> service.change(8L, request(0)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_PRIORITY_INVALID);
        verify(transactions, never()).execute(any(), any(), any());
    }

    @Test
    void completedLotRejected() {
        lot.setStatus("COMPLETED");

        assertThatThrownBy(() -> service.change(8L, request(3)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_PRIORITY_CLOSED);
    }

    private static LotPriorityRequest request(int priority) {
        LotPriorityRequest request = new LotPriorityRequest();
        request.setPriority(priority);
        return request;
    }
}
