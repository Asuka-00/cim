package com.scaffold.mes.exec.domain.service.interlock;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.share.integration.mes.base.response.TrackResourceFacts;

/**
 * TrackInContext 一次进站的检查材料
 *
 * @param lot 已加锁的 Lot
 * @param stepId 操作员选择的站点
 * @param equipmentId 设备，可空
 * @param portId Port，可空
 * @param recipeVersionId Recipe 版本，可空
 * @param facts 主数据判定
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record TrackInContext(
        Lot lot,
        Long stepId,
        Long equipmentId,
        Long portId,
        Long recipeVersionId,
        TrackResourceFacts facts) {
}
