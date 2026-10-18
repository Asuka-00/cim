package com.scaffold.mes.core.infra.repository.db.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.event.MesDomainEventDelivery;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventDeliveryRepository;
import com.scaffold.mes.core.infra.mapper.db.event.MesDomainEventDeliveryMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MesDomainEventDeliveryDbRepositoryImpl 事件投递仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_domain_event_delivery。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MesDomainEventDeliveryDbRepositoryImpl implements MesDomainEventDeliveryRepository {

    private final MesDomainEventDeliveryMapper mapper;

    private DbRepositorySupport<MesDomainEventDelivery> support() {
        return new DbRepositorySupport<>(mapper, MesDomainEventDelivery.class);
    }

    @Override
    public Optional<MesDomainEventDelivery> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<MesDomainEventDelivery> listDue(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return mapper.selectList(new LambdaQueryWrapper<MesDomainEventDelivery>()
                .eq(MesDomainEventDelivery::getStatus, "PENDING")
                .and(w -> w.isNull(MesDomainEventDelivery::getNextRetryAt)
                        .or()
                        .le(MesDomainEventDelivery::getNextRetryAt, now))
                .orderByAsc(MesDomainEventDelivery::getId)
                .last("LIMIT " + Math.max(limit, 1)));
    }

    @Override
    public boolean hasEarlierPending(MesDomainEventDelivery delivery) {
        LambdaQueryWrapper<MesDomainEventDelivery> wrapper = new LambdaQueryWrapper<MesDomainEventDelivery>()
                .eq(MesDomainEventDelivery::getSubscriber, delivery.getSubscriber())
                .eq(MesDomainEventDelivery::getAggregateType, delivery.getAggregateType())
                .eq(MesDomainEventDelivery::getStatus, "PENDING")
                .lt(MesDomainEventDelivery::getId, delivery.getId());
        if (delivery.getAggregateId() == null) {
            wrapper.isNull(MesDomainEventDelivery::getAggregateId);
        } else {
            wrapper.eq(MesDomainEventDelivery::getAggregateId, delivery.getAggregateId());
        }
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public PageDTO<MesDomainEventDelivery> page(int page, int pageSize, String subscriber, String status) {
        LambdaQueryWrapper<MesDomainEventDelivery> wrapper = support().wrapper();
        if (subscriber != null && !subscriber.isBlank()) {
            wrapper.eq(MesDomainEventDelivery::getSubscriber, subscriber);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(MesDomainEventDelivery::getStatus, status);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(MesDomainEventDelivery delivery) {
        support().save(delivery);
    }

    @Override
    public void update(MesDomainEventDelivery delivery) {
        mapper.updateById(delivery);
    }
}
