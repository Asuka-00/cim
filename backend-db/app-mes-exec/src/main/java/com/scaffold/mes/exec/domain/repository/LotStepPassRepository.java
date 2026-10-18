package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.LotStepPass;

/**
 * LotStepPassRepository 站点经过次数仓储
 * <p>
 * 按当前租户读写 mes_lot_step_pass。同一 Lot 与站点只有一行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LotStepPassRepository {

    /**
     * 按 Lot 与站点查询。
     *
     * @param lotId Lot
     * @param stepId 站点
     * @return 计数行，尚未 Track-Out 时为空
     */
    Optional<LotStepPass> find(Long lotId, Long stepId);

    /**
     * 列出某 Lot 的全部站点计数。
     *
     * @param lotId Lot
     * @return 计数行
     */
    List<LotStepPass> listByLot(Long lotId);

    /**
     * 插入。
     *
     * @param pass 计数行
     */
    void save(LotStepPass pass);

    /**
     * 更新。
     *
     * @param pass 已加载的计数行
     */
    void update(LotStepPass pass);
}
