package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.LotHistory;

/**
 * LotHistoryRepository Lot 历史仓储
 * <p>
 * 只追加 hist_mes_lot_history，不提供更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotHistoryRepository {

    /**
     * 插入一条历史。
     *
     * @param history 历史
     */
    void save(LotHistory history);

    /**
     * 按发生时间倒序列出某个 Lot 的历史。
     *
     * @param lotId Lot
     * @return 历史
     */
    List<LotHistory> listByLot(Long lotId);

    /**
     * 统计某根 Lot 进入某返工分支的次数，已撤销的不计。
     *
     * @param rootLotId 根 Lot
     * @param reworkId 返工分支
     * @return 次数
     */
    int countEnterRework(Long rootLotId, Long reworkId);

    /**
     * 取某 Lot 最近一条指定类型的历史。
     *
     * @param lotId Lot
     * @param txnType 事务类型
     * @return 历史，没有时为空
     */
    Optional<LotHistory> findLatest(Long lotId, String txnType);
}
