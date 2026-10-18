package com.scaffold.mes.core.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 连开发 MySQL 核对 M0 验收：并发取号不重复，NOWAIT 冲突立刻失败。
 * <p>
 * 未设置 mes.jdbc.url 时跳过，避免没有数据库的环境失败。
 */
class MesMysqlAcceptanceTest {

    private static String url;
    private static String user;
    private static String password;
    private static boolean reachable;

    @BeforeAll
    static void connect() {
        url = System.getProperty("mes.jdbc.url");
        user = System.getProperty("mes.jdbc.user", "root");
        password = System.getProperty("mes.jdbc.password", "");
        if (url == null || url.isBlank()) {
            return;
        }
        try (Connection connection = open()) {
            reachable = connection.isValid(3);
        } catch (SQLException ex) {
            reachable = false;
        }
    }

    @Test
    void oneHundredConcurrentAllocationsDoNotCollide() throws Exception {
        assumeTrue(reachable, "未配置可用的 mes.jdbc.url");
        long ruleId = ensureRule();
        try (Connection connection = open()) {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM mes_sequence_counter WHERE tenant_id = ? AND rule_id = ?")) {
                delete.setString(1, "accept");
                delete.setLong(2, ruleId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO mes_sequence_counter (id, tenant_id, rule_id, period_key, current_value) VALUES (?, ?, ?, '-', 0)")) {
                insert.setLong(1, 910000000000001L);
                insert.setString(2, "accept");
                insert.setLong(3, ruleId);
                insert.executeUpdate();
            }
        }

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        Set<Long> values = ConcurrentHashMap.newKeySet();
        AtomicInteger errors = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    values.add(allocateOne(ruleId));
                } catch (Exception ex) {
                    errors.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        assertThat(errors).hasValue(0);
        assertThat(values).hasSize(100);
    }

    @Test
    void nowaitConflictFailsImmediately() throws Exception {
        assumeTrue(reachable, "未配置可用的 mes.jdbc.url");
        long ruleId = ensureRule();
        try (Connection holder = open(); Connection waiter = open()) {
            holder.setAutoCommit(false);
            try (PreparedStatement statement = holder.prepareStatement(
                    "SELECT id FROM mes_sequence_rule WHERE id = ? FOR UPDATE")) {
                statement.setLong(1, ruleId);
                statement.execute();
            }
            long started = System.nanoTime();
            SQLException conflict = null;
            try (PreparedStatement statement = waiter.prepareStatement(
                    "SELECT id FROM mes_sequence_rule WHERE id = ? FOR UPDATE NOWAIT")) {
                statement.setLong(1, ruleId);
                statement.execute();
            } catch (SQLException ex) {
                conflict = ex;
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            holder.rollback();
            if (conflict == null) {
                throw new AssertionError("NOWAIT 没有冲突");
            }
            assertThat(conflict.getErrorCode()).isEqualTo(3572);
            assertThat(elapsedMs).isLessThan(1000L);
        }
    }

    private static long allocateOne(long ruleId) throws Exception {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            long current;
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT current_value FROM mes_sequence_counter WHERE tenant_id = ? AND rule_id = ? AND period_key = '-' FOR UPDATE")) {
                select.setString(1, "accept");
                select.setLong(2, ruleId);
                try (ResultSet rows = select.executeQuery()) {
                    if (!rows.next()) {
                        throw new SQLException("计数行不存在");
                    }
                    current = rows.getLong(1);
                }
            }
            long next = current + 1;
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE mes_sequence_counter SET current_value = ? WHERE tenant_id = ? AND rule_id = ? AND period_key = '-'")) {
                update.setLong(1, next);
                update.setString(2, "accept");
                update.setLong(3, ruleId);
                update.executeUpdate();
            }
            connection.commit();
            return next;
        }
    }

    private static long ensureRule() throws SQLException {
        try (Connection connection = open()) {
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id FROM mes_sequence_rule WHERE tenant_id = ? AND rule_code = 'LOOP_ACCEPT'")) {
                select.setString(1, "accept");
                try (ResultSet rows = select.executeQuery()) {
                    if (rows.next()) {
                        return rows.getLong(1);
                    }
                }
            }
            long id = 910000000000000L;
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO mes_sequence_rule (id, tenant_id, rule_code, rule_name, biz_type, seq_length, seq_start, seq_step, reset_cycle, enabled) VALUES (?, ?, 'LOOP_ACCEPT', '验收', 'LOT', 5, 1, 1, 'NEVER', 1)")) {
                insert.setLong(1, id);
                insert.setString(2, "accept");
                insert.executeUpdate();
            }
            return id;
        }
    }

    private static Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
