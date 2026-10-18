package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.scaffold.mes.base.domain.entity.RouteRework;
import com.scaffold.mes.base.domain.repository.RouteReworkRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteReworkMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteReworkDbRepositoryImpl 返工分支仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteReworkDbRepositoryImpl implements RouteReworkRepository {

    private final RouteReworkMapper mapper;

    @Override
    public Optional<RouteRework> findById(Long id) {
        return new DbRepositorySupport<>(mapper, RouteRework.class).findById(id);
    }
}
