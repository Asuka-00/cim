package com.scaffold.mes.core.domain.repository.txn;

import com.scaffold.mes.core.domain.entity.txn.MesTxnReversal;

/**
 * MesTxnReversalRepository 撤销登记仓储
 * <p>
 * 只插入撤销关系。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesTxnReversalRepository {

    /**
     * 插入记录。
     *
     * @param reversal 待插入对象
     */
    void save(MesTxnReversal reversal);

    /**
     * 原事务是否已登记撤销。
     *
     * @param origTxnId 原事务
     * @return 已撤销时为 true
     */
    boolean existsByOrigTxnId(Long origTxnId);
}
