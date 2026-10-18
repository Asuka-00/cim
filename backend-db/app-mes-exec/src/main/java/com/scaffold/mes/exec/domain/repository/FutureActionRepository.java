package com.scaffold.mes.exec.domain.repository;

import java.util.List;

import com.scaffold.mes.exec.domain.entity.FutureAction;

/**
 * FutureActionRepository 预约动作仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface FutureActionRepository {

    /**
     * 插入。
     *
     * @param action 预约
     */
    void save(FutureAction action);

    /**
     * 更新。
     *
     * @param action 已加载的预约
     */
    void update(FutureAction action);

    /**
     * 列出某 Lot 在某步骤仍待执行的到站动作。
     *
     * @param lotId Lot
     * @param routeVersionId Route 版本
     * @param routeSeqNo 主流程序号
     * @return PENDING 且时机为 ON_ARRIVE 的记录
     */
    List<FutureAction> listDue(Long lotId, Long routeVersionId, Integer routeSeqNo);

    /**
     * 列出某 Lot 在某个 Route 版本上仍待执行的预约。
     *
     * @param lotId Lot
     * @param routeVersionId Route 版本
     * @return PENDING 记录
     */
    List<FutureAction> listPending(Long lotId, Long routeVersionId);

    /**
     * 列出某个 Lot 的全部预约。
     *
     * @param lotId Lot
     * @return 按步骤序号排列
     */
    List<FutureAction> listByLot(Long lotId);
}