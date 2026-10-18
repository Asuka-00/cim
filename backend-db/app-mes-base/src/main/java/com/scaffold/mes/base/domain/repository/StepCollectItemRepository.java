package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.StepCollectItem;

/**
 * StepCollectItemRepository 站点采集规格仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepCollectItemRepository {

    /**
     * 按站点、采集项和产品查询。
     *
     * @param stepId 站点
     * @param itemId 采集项
     * @param productId 产品。0 表示通用
     * @return 规格，不存在时为空
     */
    Optional<StepCollectItem> find(Long stepId, Long itemId, Long productId);

    /**
     * 插入。
     *
     * @param item 规格
     */
    void save(StepCollectItem item);

    /**
     * 列出这个站点的规格。
     *
     * @param stepId 站点
     * @return 规格，没有时为空列表
     */
    List<StepCollectItem> listByStep(Long stepId);
}
