package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.scaffold.mes.exec.domain.entity.GenealogyLink;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * GenealogyLinkMapper 追溯关系 Mapper
 * <p>
 * 映射 hist_mes_genealogy_link。txn_id 的回填用专用 SQL，不走通用更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface GenealogyLinkMapper extends BatchBaseMapper<GenealogyLink> {

    /**
     * 原事务已在撤销登记中时，只改写 txn_id。
     *
     * @param id 关系主键
     * @param txnId 新事务
     * @return 改写行数
     */
    @Update("""
            UPDATE hist_mes_genealogy_link
            SET txn_id = #{txnId}
            WHERE id = #{id}
              AND EXISTS (
                SELECT 1 FROM hist_mes_txn_reversal r
                WHERE r.tenant_id = hist_mes_genealogy_link.tenant_id
                  AND r.orig_txn_id = hist_mes_genealogy_link.txn_id)
            """)
    int backfillTxnId(@Param("id") Long id, @Param("txnId") Long txnId);
}
