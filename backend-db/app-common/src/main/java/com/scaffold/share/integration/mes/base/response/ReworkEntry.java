package com.scaffold.share.integration.mes.base.response;

/**
 * ReworkEntry 可以进入的返工分支
 * <p>
 * 次数上限按根 Lot 的历史计算，不存在分支上。
 *
 * @param id 返工分支
 * @param routeVersionId 所属 Route 版本
 * @param entryFromSeq 允许进入的主流程序号起
 * @param entryToSeq 允许进入的主流程序号止
 * @param returnSeqNo 走完后回到的主流程序号
 * @param maxCount 最大次数
 * @param enabled 是否启用
 * @param firstSeqNo 返工内第一步序号
 * @param firstStepId 返工内第一步站点
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record ReworkEntry(
        Long id,
        Long routeVersionId,
        int entryFromSeq,
        int entryToSeq,
        int returnSeqNo,
        int maxCount,
        boolean enabled,
        Integer firstSeqNo,
        Long firstStepId) {
}
