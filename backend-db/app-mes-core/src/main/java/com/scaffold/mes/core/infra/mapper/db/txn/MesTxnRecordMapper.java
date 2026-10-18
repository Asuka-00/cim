package com.scaffold.mes.core.infra.mapper.db.txn;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.txn.MesTxnRecord;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MesTxnRecordMapper 事务流水 Mapper
 * <p>
 * 映射 hist_mes_transaction。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MesTxnRecordMapper extends BatchBaseMapper<MesTxnRecord> {
}
