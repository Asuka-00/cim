package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.infra.mapper.db.RouteStepMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * RouteStepDbRepositoryImpl Route 步骤仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_route_step。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class RouteStepDbRepositoryImpl implements RouteStepRepository {

    private final RouteStepMapper mapper;

    private DbRepositorySupport<RouteStep> support() {
        return new DbRepositorySupport<>(mapper, RouteStep.class);
    }

    @Override
    public Optional<RouteStep> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<RouteStep> findBySeq(Long routeVersionId, Integer seqNo) {
        return support().findOne(new LambdaQueryWrapper<RouteStep>()
                .eq(RouteStep::getRouteVersionId, routeVersionId)
                .eq(RouteStep::getSeqNo, seqNo));
    }

    @Override
    public List<RouteStep> listByVersion(Long routeVersionId) {
        return mapper.selectList(new LambdaQueryWrapper<RouteStep>()
                .eq(RouteStep::getRouteVersionId, routeVersionId)
                .orderByAsc(RouteStep::getSeqNo));
    }

    @Override
    public void save(RouteStep step) {
        support().save(step);
    }

    @Override
    public void update(RouteStep step) {
        support().update(step);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }
}
