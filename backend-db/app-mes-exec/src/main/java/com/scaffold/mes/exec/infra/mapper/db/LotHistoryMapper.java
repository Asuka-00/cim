package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotHistoryMapper Lot 历史 Mapper
 * <p>
 * 映射 hist_mes_lot_history。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotHistoryMapper extends BatchBaseMapper<LotHistory> {

    /**
     * 统计未撤销的进入返工次数。历史行还没有事务号时也计入。
     *
     * @param rootLotId 根 Lot
     * @param reworkId 返工分支
     * @return 次数
     */
    @Select("""
            SELECT COUNT(1) FROM hist_mes_lot_history h
            WHERE h.txn_type = 'ENTER_REWORK' AND h.rework_id = #{reworkId}
              AND h.lot_id IN (SELECT id FROM mes_lot WHERE id = #{rootLotId} OR root_lot_id = #{rootLotId})
              AND NOT EXISTS (
                SELECT 1 FROM hist_mes_txn_reversal r
                WHERE r.tenant_id = h.tenant_id AND r.orig_txn_id = h.txn_id)
            """)
    int countEnterRework(@Param("rootLotId") Long rootLotId, @Param("reworkId") Long reworkId);
}
