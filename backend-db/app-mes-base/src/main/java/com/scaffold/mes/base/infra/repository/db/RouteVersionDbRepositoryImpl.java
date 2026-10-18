package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteVersionMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteVersionDbRepositoryImpl Route 版本仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_route_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteVersionDbRepositoryImpl implements RouteVersionRepository {

    private final RouteVersionMapper mapper;

    private DbRepositorySupport<RouteVersion> support() {
        return new DbRepositorySupport<>(mapper, RouteVersion.class);
    }

    @Override
    public Optional<RouteVersion> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public PageDTO<RouteVersion> page(Long routeId, int page, int pageSize) {
        return support().queryPage(page, pageSize, support().wrapper()
                .eq(RouteVersion::getRouteId, routeId)
                .orderByDesc(RouteVersion::getVersionNo));
    }

    @Override
    public List<RouteVersion> listActive(Long routeId) {
        return mapper.selectList(support().wrapper()
                .eq(RouteVersion::getRouteId, routeId)
                .eq(RouteVersion::getStatus, "ACTIVE"));
    }

    @Override
    public int nextVersionNo(Long routeId) {
        RouteVersion latest = mapper.selectOne(support().wrapper()
                .eq(RouteVersion::getRouteId, routeId)
                .orderByDesc(RouteVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    @Override
    public void save(RouteVersion version) {
        support().save(version);
    }

    @Override
    public void update(RouteVersion version) {
        support().update(version);
    }
}
