package com.scaffold.share.integration.mes.base.response;

/**
 * ReworkProgress 返工出站后的去向
 * <p>
 * 还有下一步时留在返工里。没有下一步时回到主流程。
 *
 * @param backToMain 为 true 时离开返工
 * @param reworkSeqNo 下一条返工序号。回到主流程时为空
 * @param routeSeqNo 回到的主流程序号。仍在返工时为空
 * @param stepId 下一站点
 * @param areaId 站点区域，可空
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record ReworkProgress(
        boolean backToMain,
        Integer reworkSeqNo,
        Integer routeSeqNo,
        Long stepId,
        Long areaId) {
}
