package com.scaffold.mes.core.infra.mapper.db.notify;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.notify.NotifyRule;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * NotifyRuleMapper 通知规则 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface NotifyRuleMapper extends BatchBaseMapper<NotifyRule> {
}
