package com.scaffold.share.integration.mes.base.response;

/**
 * RouteStepNext 默认的下一步
 * <p>
 * M3 只按序号取下一个步骤。返工、条件跳转和抽检不在这里。
 *
 * @param seqNo 下一步序号
 * @param stepId 下一步站点
 * @param areaId 站点所在区域，可空
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record RouteStepNext(Integer seqNo, Long stepId, Long areaId) {
}
