package com.scaffold.mes.core.domain.service.txn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventRepository;
import com.scaffold.mes.core.domain.repository.txn.MesTxnRecordRepository;
import com.scaffold.mes.core.domain.repository.txn.MesTxnReversalRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 加锁顺序,以及同一行上后到的事务立刻失败、不执行业务。
 */
class MesLockOrderTest {

    @Test
    void locksWorkOrderThenEquipmentThenLotsById() {
        List<String> order = new ArrayList<>();
        MesTransactionService service = service((kind, id) -> order.add(kind + ":" + id));
        MesTxnCommand command = command();
        command.getLocks().add(new MesLockTarget("LOT", 10));
        command.getLocks().add(new MesLockTarget("EQUIPMENT", 1));
        command.getLocks().add(new MesLockTarget("LOT", 3));
        command.getLocks().add(new MesLockTarget("WORK_ORDER", 7));

        service.execute(command, () -> "ok", String.class);

        assertThat(order).containsExactly("WORK_ORDER:7", "EQUIPMENT:1", "LOT:3", "LOT:10");
    }

    @Test
    void secondTransactionOnSameLotFailsImmediately() throws Exception {
        ReentrantLock lot = new ReentrantLock();
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger workRuns = new AtomicInteger();
        MesRowLocker locker = (kind, id) -> {
            if (!lot.tryLock()) {
                throw new BusinessException(MesErrorCodes.OBJECT_BUSY, "对象被占用");
            }
        };
        MesTransactionService service = service(locker);
        MesTxnCommand first = command();
        first.setRequestId("a");
        first.getLocks().add(new MesLockTarget("LOT", 3));
        MesTxnCommand second = command();
        second.setRequestId("b");
        second.getLocks().add(new MesLockTarget("LOT", 3));

        Thread winner = new Thread(() -> service.execute(first, () -> {
            workRuns.incrementAndGet();
            holding.countDown();
            await(release);
            lot.unlock();
            return "ok";
        }, String.class));
        winner.start();
        holding.await();

        assertThatThrownBy(() -> service.execute(second, () -> {
            workRuns.incrementAndGet();
            return "no";
        }, String.class)).isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.OBJECT_BUSY);
        assertThat(workRuns).hasValue(1);

        release.countDown();
        winner.join(5000);
    }

    private static MesTransactionService service(MesRowLocker locker) {
        MesTxnRecordRepository txnRepository = mock(MesTxnRecordRepository.class);
        when(txnRepository.findSuccessByIdemKey(any())).thenReturn(java.util.Optional.empty());
        doAnswer(invocation -> null).when(txnRepository).save(any());
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("operator");
        return new MesTransactionService(txnRepository, mock(MesTxnReversalRepository.class),
                mock(MesDomainEventRepository.class), session, new ObjectMapper(), locker, immediate());
    }

    private static MesTxnCommand command() {
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("TRACK_IN");
        command.setRequestId("req");
        command.setObjectType("LOT");
        return command;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }

    private static PlatformTransactionManager immediate() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) {
            }
        };
    }
}
