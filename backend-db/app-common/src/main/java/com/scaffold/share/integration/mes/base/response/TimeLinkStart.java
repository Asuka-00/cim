package com.scaffold.share.integration.mes.base.response;

/**
 * TimeLinkStart 从当前步骤出发的时间链
 * <p>
 * 起点出站时按最长分钟数开计时。
 *
 * @param id 约束主键
 * @param fromSeqNo 起点序号
 * @param toSeqNo 终点序号
 * @param maxMinutes 最长间隔
 * @param action HOLD 或 WARN
 * @param reasonCode 自动 Hold 原因码
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record TimeLinkStart(Long id, Integer fromSeqNo, Integer toSeqNo, Integer maxMinutes, String action,
        String reasonCode) {
}
