package com.scaffold.share.integration.mes.base.response;

/**
 * RouteStepStart Route 的第一步
 * <p>
 * 开批时 Lot 停在序号最小的步骤上。返工和跳站不在这一步里。
 *
 * @param seqNo 步骤序号
 * @param stepId 站点
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record RouteStepStart(Integer seqNo, Long stepId) {
}
