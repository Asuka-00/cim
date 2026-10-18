package com.scaffold.mes.core.infra.mapper.db.event;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.event.MesDomainEventDelivery;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MesDomainEventDeliveryMapper 事件投递 Mapper
 * <p>
 * 映射 mes_domain_event_delivery。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MesDomainEventDeliveryMapper extends BatchBaseMapper<MesDomainEventDelivery> {
}
