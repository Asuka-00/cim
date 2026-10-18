package com.scaffold.mes.core.infra.config;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

/**
 * JdbcShedLockProvider JDBC 调度锁
 * <p>
 * 用 shedlock 表保证同一任务只有一个实例在执行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public class JdbcShedLockProvider implements LockProvider {

    private static final String LOCKED_BY = ManagementFactory.getRuntimeMXBean().getName();

    private final JdbcTemplate jdbc;

    public JdbcShedLockProvider(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 尝试取得调度锁。锁已被其他实例持有时返回空。
     *
     * @param config 锁名与持有时间
     * @return 取得的锁,未取得时为空
     */
    @Override
    public Optional<SimpleLock> lock(LockConfiguration config) {
        Instant now = Instant.now();
        int updated = jdbc.update(
                "UPDATE shedlock SET lock_until = ?, locked_at = ?, locked_by = ? WHERE name = ? AND lock_until <= ?",
                java.sql.Timestamp.from(config.getLockAtMostUntil()),
                java.sql.Timestamp.from(now),
                LOCKED_BY,
                config.getName(),
                java.sql.Timestamp.from(now));
        if (updated == 0) {
            try {
                jdbc.update("INSERT INTO shedlock(name, lock_until, locked_at, locked_by) VALUES (?, ?, ?, ?)",
                        config.getName(),
                        java.sql.Timestamp.from(config.getLockAtMostUntil()),
                        java.sql.Timestamp.from(now),
                        LOCKED_BY);
            } catch (org.springframework.dao.DuplicateKeyException ex) {
                return Optional.empty();
            }
        }
        return Optional.of(() -> jdbc.update(
                "UPDATE shedlock SET lock_until = ? WHERE name = ? AND locked_by = ?",
                java.sql.Timestamp.from(config.getLockAtLeastUntil()),
                config.getName(),
                LOCKED_BY));
    }
}
