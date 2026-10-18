package com.scaffold.mes.base.infra.repository.db;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.RouteReworkStep;
import com.scaffold.mes.base.domain.repository.RouteReworkStepRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteReworkStepMapper;

import lombok.RequiredArgsConstructor;

/**
 * RouteReworkStepDbRepositoryImpl 返工步骤仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteReworkStepDbRepositoryImpl implements RouteReworkStepRepository {

    private final RouteReworkStepMapper mapper;

    @Override
    public List<RouteReworkStep> listByRework(Long reworkId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteReworkStep>().eq(RouteReworkStep::getReworkId, reworkId));
    }
}
