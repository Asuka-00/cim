package com.scaffold.share.integration.mes.base.provider;

import java.util.List;
import java.util.Optional;

import com.scaffold.share.integration.mes.base.response.CollectLimit;

/**
 * CollectSpecPort 采集规格
 * <p>
 * 执行模块不能直接读站点规格。产品专用优先于通用。没有规格时为空。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface CollectSpecPort {

    /**
     * 取站点上这个采集项的上下限。
     *
     * @param stepId 站点
     * @param itemId 采集项
     * @param productId 产品。空按通用
     * @return 上下限，没有时为空
     */
    Optional<CollectLimit> limit(Long stepId, Long itemId, Long productId);

    /**
     * 这个站点出站前必须有有效值的采集项。
     *
     * @param stepId 站点
     * @param productId 产品。空按通用
     * @return 采集项。没有必采项时为空列表
     */
    List<Long> requiredItems(Long stepId, Long productId);

    /**
     * 取这一步这个采集项的上下限。步骤上有覆盖时，不再使用站点规格。
     *
     * @param routeVersionId Route 版本
     * @param seqNo 步骤序号
     * @param stepId 站点
     * @param itemId 采集项
     * @param productId 产品。空按通用
     * @return 上下限，没有时为空
     */
    Optional<CollectLimit> limit(Long routeVersionId, Integer seqNo, Long stepId, Long itemId, Long productId);

    /**
     * 这一步出站前必须有有效值的采集项。步骤上有覆盖时，不再使用站点必采项。
     *
     * @param routeVersionId Route 版本
     * @param seqNo 步骤序号
     * @param stepId 站点
     * @param productId 产品。空按通用
     * @return 采集项。没有必采项时为空列表
     */
    List<Long> requiredItems(Long routeVersionId, Integer seqNo, Long stepId, Long productId);
}
