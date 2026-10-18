package com.scaffold.mes.core.infra.mapper.db.sequence;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.sequence.SequenceCounter;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * SequenceCounterMapper 编号流水 Mapper
 * <p>
 * 映射 mes_sequence_counter。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface SequenceCounterMapper extends BatchBaseMapper<SequenceCounter> {
}
