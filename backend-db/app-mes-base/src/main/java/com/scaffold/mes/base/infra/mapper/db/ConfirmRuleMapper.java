package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.ConfirmRule;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ConfirmRuleMapper 二次确认规则 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ConfirmRuleMapper extends BatchBaseMapper<ConfirmRule> {
}
