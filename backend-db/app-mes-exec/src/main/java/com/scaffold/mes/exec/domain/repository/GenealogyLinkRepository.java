package com.scaffold.mes.exec.domain.repository;

import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.GenealogyLink;

/**
 * GenealogyLinkRepository 追溯关系仓储
 * <p>
 * 首次插入。再次发生时不改数量和时间，只在原事务已撤销时回填 txn_id。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface GenealogyLinkRepository {

    /**
     * 按唯一键查询。
     *
     * @param parentType 上游类型
     * @param parentId 上游对象
     * @param childType 下游类型
     * @param childId 下游对象
     * @param linkType 关系类型
     * @param stepId 站点，0 表示无关
     * @return 已有关系，没有时为空
     */
    Optional<GenealogyLink> find(String parentType, Long parentId, String childType, Long childId, String linkType, Long stepId);

    /**
     * 插入。
     *
     * @param link 关系
     */
    void save(GenealogyLink link);

    /**
     * 仅当原事务已撤销时，把 txn_id 改成新事务。不改其他列。
     *
     * @param id 关系主键
     * @param txnId 新事务
     * @return 实际改写的行数
     */
    int backfillTxnId(Long id, Long txnId);
}
