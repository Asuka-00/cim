package com.scaffold.mes.core.infra.mapper.db.txn;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.txn.MesTxnReversal;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MesTxnReversalMapper 撤销登记 Mapper
 * <p>
 * 映射 hist_mes_txn_reversal。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MesTxnReversalMapper extends BatchBaseMapper<MesTxnReversal> {
}
