package com.scaffold.share.integration.mes.base.response;

/**
 * QueueTimeLimit 步骤排队时间
 * <p>
 * maxMinutes 为空表示不限制。action 为 HOLD 或 WARN。
 *
 * @param maxMinutes 进入站点到进站的最长等待，分钟
 * @param action 超时动作
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record QueueTimeLimit(Integer maxMinutes, String action) {
}
