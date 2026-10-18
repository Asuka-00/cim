package com.scaffold.mes.core.infra.repository.db.event;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventRepository;
import com.scaffold.mes.core.infra.mapper.db.event.MesDomainEventMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MesDomainEventDbRepositoryImpl 领域事件仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_domain_event。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MesDomainEventDbRepositoryImpl implements MesDomainEventRepository {

    private final MesDomainEventMapper mapper;

    private DbRepositorySupport<MesDomainEvent> support() {
        return new DbRepositorySupport<>(mapper, MesDomainEvent.class);
    }

    @Override
    public Optional<MesDomainEvent> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public List<MesDomainEvent> listNew(int limit) {
        return mapper.selectList(new LambdaQueryWrapper<MesDomainEvent>()
                .eq(MesDomainEvent::getStatus, "NEW")
                .orderByAsc(MesDomainEvent::getId)
                .last("LIMIT " + Math.max(limit, 1)));
    }

    @Override
    public PageDTO<MesDomainEvent> page(int page, int pageSize, String status) {
        LambdaQueryWrapper<MesDomainEvent> wrapper = support().wrapper();
        if (status != null && !status.isBlank()) {
            wrapper.eq(MesDomainEvent::getStatus, status);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(MesDomainEvent event) {
        support().save(event);
    }

    @Override
    public void update(MesDomainEvent event) {
        mapper.updateById(event);
    }
}
