package com.scaffold.share.utils;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ScoreMath 评分数学工具
 * <p>
 * 收敛评分/守卫 handler 中重复的归一与时间换算口径：[0,1] 截断、两时刻间小时差与秒差。
 * 全部为无状态静态方法，线程安全。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-23
 */
public final class ScoreMath {

    private ScoreMath() {
    }

    /**
     * 将值截断到 [0,1]。
     *
     * @param v 输入值
     * @return 截断后的值
     */
    public static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /**
     * 两时刻间的小时差（{@code from→to}），按秒折算（toSeconds()/3600.0）。
     *
     * @param from 起点
     * @param to   终点
     * @return 小时差
     */
    public static double hoursBetween(LocalDateTime from, LocalDateTime to) {
        return Duration.between(from, to).toSeconds() / 3600.0;
    }

    /**
     * 两时刻间的秒差（{@code from→to}），即 toSeconds()。
     *
     * @param from 起点
     * @param to   终点
     * @return 秒差
     */
    public static long secondsBetween(LocalDateTime from, LocalDateTime to) {
        return Duration.between(from, to).toSeconds();
    }
}
