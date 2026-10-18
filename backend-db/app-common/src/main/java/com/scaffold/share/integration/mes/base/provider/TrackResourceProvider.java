package com.scaffold.share.integration.mes.base.provider;

import com.scaffold.share.integration.mes.base.response.TrackResourceFacts;

/**
 * TrackResourceProvider 进站资源查询
 * <p>
 * 由 app-mes-base 实现。执行模块进站前调用一次。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface TrackResourceProvider {

    /**
     * 读取站点、设备和 Recipe 的当前判定。
     *
     * @param stepId 站点
     * @param routeVersionId Route 版本
     * @param routeSeqNo 步骤序号
     * @param productId 产品
     * @param equipmentId 设备，站点不要求设备时可以空
     * @param portId Port，可空
     * @param recipeVersionId Recipe 版本，可空
     * @return 判定材料
     */
    TrackResourceFacts facts(Long stepId, Long routeVersionId, Integer routeSeqNo, Long productId,
            Long equipmentId, Long portId, Long recipeVersionId);
}
