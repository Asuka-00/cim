package com.scaffold.mes.core.domain.service.txn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.entity.txn.MesTxnRecord;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventRepository;
import com.scaffold.mes.core.domain.repository.txn.MesTxnRecordRepository;
import com.scaffold.mes.core.domain.repository.txn.MesTxnReversalRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 统一事务的成功幂等与失败留痕。不连接数据库。
 */
class MesTransactionServiceTest {

    private final List<MesTxnRecord> saved = new ArrayList<>();
    private MesTxnRecordRepository txnRepository;
    private MesTxnReversalRepository reversalRepository;
    private MesTransactionService service;

    @BeforeEach
    void setUp() {
        txnRepository = mock(MesTxnRecordRepository.class);
        reversalRepository = mock(MesTxnReversalRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("operator");
        when(txnRepository.findSuccessByIdemKey(any())).thenAnswer(invocation -> saved.stream()
                .filter(record -> invocation.getArgument(0).equals(record.getIdemKey()))
                .filter(record -> "SUCCESS".equals(record.getResult()))
                .findFirst());
        doAnswer(invocation -> {
            saved.add(invocation.getArgument(0));
            return null;
        }).when(txnRepository).save(any());
        service = new MesTransactionService(txnRepository, reversalRepository,
                mock(MesDomainEventRepository.class), session, new ObjectMapper(), (kind, id) -> {
                }, immediateTransactionManager());
    }

    @Test
    void sameRequestIdRunsWorkOnceAndReplaysResponse() {
        AtomicInteger runs = new AtomicInteger();
        MesTxnCommand command = command();

        String first = service.execute(command, () -> {
            runs.incrementAndGet();
            return "L261000001";
        }, String.class);
        String second = service.execute(command, () -> {
            runs.incrementAndGet();
            return "SHOULD_NOT_RUN";
        }, String.class);

        assertThat(first).isEqualTo("L261000001");
        assertThat(second).isEqualTo("L261000001");
        assertThat(runs).hasValue(1);
        assertThat(saved).singleElement().extracting(MesTxnRecord::getResult).isEqualTo("SUCCESS");
    }

    @Test
    void failedWorkKeepsAFailedRecordAndCanRetry() {
        MesTxnCommand command = command();

        assertThatThrownBy(() -> service.execute(command, () -> {
            throw new IllegalStateException("校验失败");
        }, String.class)).isInstanceOf(IllegalStateException.class);

        assertThat(saved).singleElement().satisfies(record -> {
            assertThat(record.getResult()).isEqualTo("FAILED");
            assertThat(record.getIdemKey()).isNull();
            assertThat(record.getErrorMessage()).contains("校验失败");
        });

        String retried = service.execute(command, () -> "L261000002", String.class);
        assertThat(retried).isEqualTo("L261000002");
        verify(txnRepository, times(2)).save(any());
    }

    @Test
    void reverseRejectsMissingTransaction() {
        when(txnRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reverse(99L, "MISTAKE"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.TXN_NOT_FOUND);
    }

    @Test
    void secondReverseOfSameTransactionIsRejected() {
        MesTxnRecord original = new MesTxnRecord();
        original.setId(7L);
        original.setTxnType("TRACK_OUT");
        original.setObjectType("LOT");
        original.setObjectId(3L);
        when(txnRepository.findById(7L)).thenReturn(Optional.of(original));
        org.mockito.Mockito.doThrow(new DuplicateKeyException("uk_orig_txn")).when(reversalRepository).save(any());

        assertThatThrownBy(() -> service.reverse(7L, "MISTAKE"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesErrorCodes.TXN_ALREADY_REVERSED);
    }

    private static MesTxnCommand command() {
        MesTxnCommand command = new MesTxnCommand();
        command.setTxnType("CREATE_LOT");
        command.setRequestId("req-1");
        command.setObjectType("LOT");
        return command;
    }

    private static PlatformTransactionManager immediateTransactionManager() {
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
