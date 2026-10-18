package com.scaffold.mes.core.infra.repository.db.txn;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.txn.MesTxnReversal;
import com.scaffold.mes.core.domain.repository.txn.MesTxnReversalRepository;
import com.scaffold.mes.core.infra.mapper.db.txn.MesTxnReversalMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MesTxnReversalDbRepositoryImpl 撤销登记仓储实现
 * <p>
 * 只增表,不提供更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MesTxnReversalDbRepositoryImpl implements MesTxnReversalRepository {

    private final MesTxnReversalMapper mapper;

    @Override
    public void save(MesTxnReversal reversal) {
        new AppendOnlyRepositorySupport<>(mapper, MesTxnReversal.class).save(reversal);
    }

    @Override
    public boolean existsByOrigTxnId(Long origTxnId) {
        if (origTxnId == null) {
            return false;
        }
        Long count = mapper.selectCount(new LambdaQueryWrapper<MesTxnReversal>().eq(MesTxnReversal::getOrigTxnId, origTxnId));
        return count != null && count > 0;
    }
}
