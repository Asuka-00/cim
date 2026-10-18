package com.scaffold.mes.base.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.RouteStepCollectItem;
import com.scaffold.mes.base.domain.repository.RouteStepCollectItemRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteStepCollectItemMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepCollectItemDbRepositoryImpl Route 步骤采集规格仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteStepCollectItemDbRepositoryImpl implements RouteStepCollectItemRepository {

    private final RouteStepCollectItemMapper mapper;

    @Override
    public List<RouteStepCollectItem> listByRouteStep(Long routeStepId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStepCollectItem>()
                .eq(RouteStepCollectItem::getRouteStepId, routeStepId));
    }

    @Override
    public List<RouteStepCollectItem> listByVersion(Long routeVersionId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStepCollectItem>()
                .eq(RouteStepCollectItem::getRouteVersionId, routeVersionId));
    }

    @Override
    public void save(RouteStepCollectItem item) {
        new DbRepositorySupport<>(mapper, RouteStepCollectItem.class).save(item);
    }
}