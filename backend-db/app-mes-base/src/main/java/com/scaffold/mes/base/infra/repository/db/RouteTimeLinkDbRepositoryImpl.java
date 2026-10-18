package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.RouteTimeLink;
import com.scaffold.mes.base.domain.repository.RouteTimeLinkRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteTimeLinkMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteTimeLinkDbRepositoryImpl Time Link 仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_route_time_link。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteTimeLinkDbRepositoryImpl implements RouteTimeLinkRepository {

    private final RouteTimeLinkMapper mapper;

    private DbRepositorySupport<RouteTimeLink> support() {
        return new DbRepositorySupport<>(mapper, RouteTimeLink.class);
    }

    @Override
    public Optional<RouteTimeLink> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<RouteTimeLink> findExact(Long routeVersionId, Integer fromSeqNo, Integer toSeqNo) {
        return support().findOne(new LambdaQueryWrapper<RouteTimeLink>()
                .eq(RouteTimeLink::getRouteVersionId, routeVersionId)
                .eq(RouteTimeLink::getFromSeqNo, fromSeqNo)
                .eq(RouteTimeLink::getToSeqNo, toSeqNo));
    }

    @Override
    public List<RouteTimeLink> listByVersion(Long routeVersionId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteTimeLink>()
                .eq(RouteTimeLink::getRouteVersionId, routeVersionId)
                .orderByAsc(RouteTimeLink::getFromSeqNo)
                .orderByAsc(RouteTimeLink::getToSeqNo));
    }

    @Override
    public void save(RouteTimeLink link) {
        support().save(link);
    }

    @Override
    public void update(RouteTimeLink link) {
        support().update(link);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
