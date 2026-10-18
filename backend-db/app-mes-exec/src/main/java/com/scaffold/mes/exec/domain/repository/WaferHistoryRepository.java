package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.WaferHistory;

/**
 * WaferHistoryRepository Wafer 历史仓储
 * <p>
 * 只追加 hist_mes_wafer_history，不提供更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface WaferHistoryRepository {

    /**
     * 插入一条历史。
     *
     * @param history 历史
     */
    void save(WaferHistory history);

    /**
     * 按发生时间倒序列出某片的历史。
     *
     * @param waferId Wafer
     * @return 历史
     */
    List<WaferHistory> listByWafer(Long waferId);

    /**
     * 取某片最近一条指定类型的历史。
     *
     * @param waferId Wafer
     * @param txnType 事务类型
     * @return 历史，没有时为空
     */
    Optional<WaferHistory> findLatest(Long waferId, String txnType);
}
