package com.scaffold.share.utils;

import java.time.LocalDateTime;

/**
 * ActivePredicates 生效窗口判定工具。
 * <p>
 * 统一 {@code [effectiveFrom, effectiveTo)} 半开区间判定，供各含生效窗的仓储实现复用，
 * 避免窗口逻辑在多处拷贝导致漂移。{@code now} 作为入参传入，便于测试。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-22
 */
public final class ActivePredicatesUtils {

    private ActivePredicatesUtils() {
    }

    /**
     * 判断 {@code now} 是否落在生效窗口 {@code [from, to)} 内。
     * <p>
     * {@code from} 为 null 表示无开始限制；{@code to} 为 null 表示永不过期。
     *
     * @param from 生效开始时间，可为 null
     * @param to   生效结束时间，可为 null
     * @param now  判定基准时间
     * @return 是否落在窗口内
     */
    public static boolean withinWindow(LocalDateTime from, LocalDateTime to, LocalDateTime now) {
        boolean afterStart = from == null || !now.isBefore(from);
        boolean beforeEnd = to == null || now.isBefore(to);
        return afterStart && beforeEnd;
    }
}
