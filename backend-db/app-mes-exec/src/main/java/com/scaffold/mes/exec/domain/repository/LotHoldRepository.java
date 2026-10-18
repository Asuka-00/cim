package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.LotHold;

/**
 * LotHoldRepository Lot Hold 仓储
 * <p>
 * 按当前租户读写 mes_lot_hold。系统 Hold 先按 active_key 查已生效的记录。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotHoldRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return Hold，不存在时为空
     */
    Optional<LotHold> findById(Long id);

    /**
     * 列出某 Lot 仍生效的 Hold。
     *
     * @param lotId Lot
     * @return ACTIVE 记录
     */
    List<LotHold> listActive(Long lotId);

    /**
     * 按幂等键查询仍生效的 Hold。
     *
     * @param activeKey 幂等键
     * @return ACTIVE 记录，没有时为空
     */
    Optional<LotHold> findActiveByKey(String activeKey);

    /**
     * 插入。
     *
     * @param hold Hold
     */
    void save(LotHold hold);

    /**
     * 更新。
     *
     * @param hold 已加载的 Hold
     */
    void update(LotHold hold);
}
