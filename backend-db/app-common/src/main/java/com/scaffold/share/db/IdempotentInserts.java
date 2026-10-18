package com.scaffold.share.db;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * IdempotentInserts 冷写落库幂等插入
 * <p>
 * Kafka 落库是至少一次投递：已写库但 offset 未提交的消息在重启 / 再均衡后会重放。
 * 主键冲突说明该记录已落库，按成功跳过；业务唯一键冲突仍抛出（进入重试 / 死信）。
 * 批量插入遇主键冲突时退回逐条插入，只跳过已存在的行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-09
 */
public final class IdempotentInserts {

    private static final Logger log = LoggerFactory.getLogger(IdempotentInserts.class);

    private static final long WARN_EVERY = 1000;
    private static final AtomicLong SKIPPED = new AtomicLong();

    private IdempotentInserts() {
    }

    public static <T> void insert(BatchBaseMapper<T> mapper, T entity) {
        try {
            mapper.insert(entity);
        } catch (RuntimeException ex) {
            if (!DuplicateKeys.isPrimaryKeyViolation(ex)) {
                throw ex;
            }
            long skipped = SKIPPED.incrementAndGet();
            if (skipped == 1 || skipped % WARN_EVERY == 0) {
                log.warn("Skipped replayed insert, primary key already exists: type={}, skippedTotal={}",
                        entity.getClass().getSimpleName(), skipped);
            }
        }
    }

    public static <T> void insertAll(BatchBaseMapper<T> mapper, List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        try {
            mapper.insertBatchSomeColumn(entities);
        } catch (RuntimeException ex) {
            if (!DuplicateKeys.isPrimaryKeyViolation(ex)) {
                throw ex;
            }
            for (T entity : entities) {
                insert(mapper, entity);
            }
        }
    }
}
