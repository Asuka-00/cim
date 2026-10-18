package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.BatchLot;

/**
 * BatchLotRepository 批次成员仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface BatchLotRepository {

    /**
     * 按批次和 Lot 查询，包括已移出的行。
     *
     * @param batchId 批次
     * @param lotId Lot
     * @return 成员，没有时为空
     */
    Optional<BatchLot> find(Long batchId, Long lotId);

    /**
     * 列出仍在批内的成员，按序号。
     *
     * @param batchId 批次
     * @return IN 成员
     */
    List<BatchLot> listIn(Long batchId);

    /**
     * 插入。
     *
     * @param member 成员
     */
    void save(BatchLot member);

    /**
     * 更新。
     *
     * @param member 已加载的成员
     */
    void update(BatchLot member);
}
