package com.scaffold.mes.core.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;

/**
 * 同一任务名在锁未到期时只能有一个持有者。用内存库，不连接开发 MySQL。
 */
class JdbcShedLockProviderTest {

    private JdbcShedLockProvider provider;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:shedlock;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE shedlock (
                    name VARCHAR(64) PRIMARY KEY,
                    lock_until TIMESTAMP NOT NULL,
                    locked_at TIMESTAMP NOT NULL,
                    locked_by VARCHAR(255) NOT NULL
                )
                """);
        provider = new JdbcShedLockProvider(jdbc);
    }

    @Test
    void secondAcquireFailsUntilTheFirstUnlocks() {
        LockConfiguration config = new LockConfiguration(Instant.now(), "mesEventDispatch", Duration.ofMinutes(5),
                Duration.ZERO);

        SimpleLock first = provider.lock(config).orElseThrow();
        assertThat(provider.lock(config)).isEmpty();

        first.unlock();

        assertThat(provider.lock(config)).isPresent();
    }
}
