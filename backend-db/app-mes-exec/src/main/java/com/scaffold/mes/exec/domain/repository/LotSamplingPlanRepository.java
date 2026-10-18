package com.scaffold.mes.exec.domain.repository;

import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.LotSamplingPlan;

/**
 * LotSamplingPlanRepository Lot 抽检决定仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotSamplingPlanRepository {

    /**
     * 按 Lot、步骤序号和经过次数查询。
     *
     * @param lotId Lot
     * @param routeSeqNo 步骤序号
     * @param passNo 经过次数
     * @return 决定，不存在时为空
     */
    Optional<LotSamplingPlan> find(Long lotId, Integer routeSeqNo, Integer passNo);

    /**
     * 插入。
     *
     * @param plan 决定
     */
    void save(LotSamplingPlan plan);

    /**
     * 删除这个 Lot 在指定序号之后的抽检决定。
     *
     * @param lotId Lot
     * @param routeSeqNo 出站前的步骤序号。不删除这个序号上的决定
     */
    void deleteAfter(Long lotId, Integer routeSeqNo);
}
