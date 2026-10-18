package com.scaffold.mes.core.infra.mapper.db.sequence;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * SequenceRuleMapper 编号规则 Mapper
 * <p>
 * 映射 mes_sequence_rule。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface SequenceRuleMapper extends BatchBaseMapper<SequenceRule> {
}
