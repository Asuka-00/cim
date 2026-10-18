package com.scaffold.mes.base.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.RouteStepTool;
import com.scaffold.mes.base.domain.repository.RouteStepToolRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteStepToolMapper;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepToolDbRepositoryImpl Route 步骤工具要求仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteStepToolDbRepositoryImpl implements RouteStepToolRepository {

    private final RouteStepToolMapper mapper;

    @Override
    public List<RouteStepTool> listByRouteStep(Long routeStepId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStepTool>().eq(RouteStepTool::getRouteStepId, routeStepId));
    }
}
