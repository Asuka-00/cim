package com.scaffold.mes.base.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.RouteStepSampling;
import com.scaffold.mes.base.domain.repository.RouteStepSamplingRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteStepSamplingMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepSamplingDbRepositoryImpl Route 步骤抽检覆盖仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteStepSamplingDbRepositoryImpl implements RouteStepSamplingRepository {

    private final RouteStepSamplingMapper mapper;

    @Override
    public List<RouteStepSampling> listByRouteStep(Long routeStepId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStepSampling>()
                .eq(RouteStepSampling::getRouteStepId, routeStepId));
    }

    @Override
    public List<RouteStepSampling> listByVersion(Long routeVersionId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStepSampling>()
                .eq(RouteStepSampling::getRouteVersionId, routeVersionId));
    }

    @Override
    public void save(RouteStepSampling sampling) {
        new DbRepositorySupport<>(mapper, RouteStepSampling.class).save(sampling);
    }
}