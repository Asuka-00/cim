package com.scaffold.share.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * ActivePredicatesTest 生效窗口判定单元测试。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-22
 */
class ActivePredicatesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 22, 12, 0, 0);

    @Test
    void withinWindowTreatsNullBoundsAsUnbounded() {
        assertThat(ActivePredicatesUtils.withinWindow(null, null, NOW)).isTrue();
        assertThat(ActivePredicatesUtils.withinWindow(NOW.minusDays(1), null, NOW)).isTrue();
        assertThat(ActivePredicatesUtils.withinWindow(null, NOW.plusDays(1), NOW)).isTrue();
    }

    @Test
    void withinWindowRespectsBothBounds() {
        assertThat(ActivePredicatesUtils.withinWindow(NOW.minusDays(1), NOW.plusDays(1), NOW)).isTrue();
        assertThat(ActivePredicatesUtils.withinWindow(NOW.plusDays(1), NOW.plusDays(2), NOW)).isFalse();
        assertThat(ActivePredicatesUtils.withinWindow(NOW.minusDays(2), NOW.minusDays(1), NOW)).isFalse();
    }

    @Test
    void withinWindowIsHalfOpen() {
        // from 边界包含（now == from 视为生效）
        assertThat(ActivePredicatesUtils.withinWindow(NOW, NOW.plusDays(1), NOW)).isTrue();
        // to 边界排除（now == to 视为已过期）
        assertThat(ActivePredicatesUtils.withinWindow(NOW.minusDays(1), NOW, NOW)).isFalse();
    }
}
