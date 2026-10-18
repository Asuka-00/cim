package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.MaterialConsumption;

/**
 * MaterialConsumptionRepository 物料消耗仓储
 * <p>
 * 只追加 hist_mes_material_consumption。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MaterialConsumptionRepository {

    /**
     * 插入一条消耗。
     *
     * @param consumption 消耗
     */
    void save(MaterialConsumption consumption);

    /**
     * 列出这个 Lot 的消耗。
     *
     * @param lotId Lot
     * @return 消耗，没有时为空列表
     */
    List<MaterialConsumption> listByLot(Long lotId);

    /**
     * 按主键查询。
     *
     * @param id 消耗
     * @return 消耗，不存在时为空
     */
    Optional<MaterialConsumption> findById(Long id);

    /**
     * 这笔记账是否已经有清偿记录。
     *
     * @param debtId 欠账
     * @return 已有 SETTLE 指向它时为 true
     */
    boolean existsSettle(Long debtId);
}
