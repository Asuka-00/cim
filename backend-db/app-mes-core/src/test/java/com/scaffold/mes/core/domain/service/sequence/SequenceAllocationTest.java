package com.scaffold.mes.core.domain.service.sequence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.scaffold.mes.core.domain.entity.sequence.SequenceCounter;
import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.mes.core.domain.repository.sequence.SequenceCounterRepository;
import com.scaffold.mes.core.domain.repository.sequence.SequenceRuleRepository;

/**
 * 100 次并发取号不重复。计数仓储用互斥锁模拟行锁,不连接 MySQL。
 */
class SequenceAllocationTest {

    @Test
    void oneHundredConcurrentAllocationsAreUnique() throws Exception {
        SequenceRule rule = new SequenceRule();
        rule.setId(1L);
        rule.setRuleCode("LOT");
        rule.setPrefix("L");
        rule.setDatePattern("yyMM");
        rule.setSeqLength(5);
        rule.setSeqStart(1L);
        rule.setSeqStep(1);
        rule.setResetCycle("MONTH");
        rule.setEnabled(1);
        SequenceRuleApplicationService service = new SequenceRuleApplicationService(
                ruleRepository(rule), new HoldingCounter(), immediateTransactionManager());

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        Set<String> numbers = ConcurrentHashMap.newKeySet();
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    numbers.add(service.next("LOT"));
                } catch (Throwable ex) {
                    errors.add(ex);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(errors).isEmpty();
        assertThat(numbers).hasSize(100);
    }

    private static SequenceRuleRepository ruleRepository(SequenceRule rule) {
        return new SequenceRuleRepository() {
            @Override
            public Optional<SequenceRule> findById(Long id) {
                return Optional.of(rule);
            }

            @Override
            public Optional<SequenceRule> findByRuleCode(String ruleCode) {
                return Optional.of(rule);
            }

            @Override
            public com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO<SequenceRule> page(
                    int page, int pageSize, String ruleCode) {
                return null;
            }

            @Override
            public void save(SequenceRule saved) {
            }

            @Override
            public void update(SequenceRule saved) {
            }

            @Override
            public void deleteById(Long id) {
            }
        };
    }

    /**
     * 模拟 FOR UPDATE:同一计数行同时只允许一个取号线程进入,直到 update 释放。
     */
    private static final class HoldingCounter implements SequenceCounterRepository {

        private final ReentrantLock lock = new ReentrantLock();
        private SequenceCounter row;

        @Override
        public SequenceCounter lock(Long ruleId, String periodKey) {
            if (!lock.isHeldByCurrentThread()) {
                lock.lock();
            }
            return row;
        }

        @Override
        public void save(SequenceCounter counter) {
            row = counter;
        }

        @Override
        public void update(SequenceCounter counter) {
            row = counter;
            while (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
