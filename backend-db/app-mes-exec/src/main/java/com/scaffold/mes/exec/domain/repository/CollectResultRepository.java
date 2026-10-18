package com.scaffold.mes.exec.domain.repository;

import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.CollectResult;

/**
 * CollectResultRepository 采集结果仓储
 * <p>
 * 只追加 hist_mes_collect_result。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface CollectResultRepository {

    /**
     * 插入一条原始采集值。
     *
     * @param result 采集结果
     */
    void save(CollectResult result);

    /**
     * 取这个采集点时间上的最后一条。
     *
     * @param lotId Lot
     * @param itemId 采集项
     * @param routeSeqNo 步骤序号
     * @param passNo 经过次数
     * @return 最后一条，没有时为空
     */
    Optional<CollectResult> latest(Long lotId, Long itemId, Integer routeSeqNo, Integer passNo);
}
