package com.scaffold.mes.core.infra.mapper.db.event;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MesDomainEventMapper 领域事件 Mapper
 * <p>
 * 映射 mes_domain_event。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MesDomainEventMapper extends BatchBaseMapper<MesDomainEvent> {
}
