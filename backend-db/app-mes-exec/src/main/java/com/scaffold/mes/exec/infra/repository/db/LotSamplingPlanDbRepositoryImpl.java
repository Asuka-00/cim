package com.scaffold.mes.exec.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.LotSamplingPlan;
import com.scaffold.mes.exec.domain.repository.LotSamplingPlanRepository;
import com.scaffold.mes.exec.infra.mapper.db.LotSamplingPlanMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * LotSamplingPlanDbRepositoryImpl Lot 抽检决定仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class LotSamplingPlanDbRepositoryImpl implements LotSamplingPlanRepository {

    private final LotSamplingPlanMapper mapper;

    @Override
    public Optional<LotSamplingPlan> find(Long lotId, Integer routeSeqNo, Integer passNo) {
        return new DbRepositorySupport<>(mapper, LotSamplingPlan.class)
                .findOne(new LambdaQueryWrapper<LotSamplingPlan>()
                        .eq(LotSamplingPlan::getLotId, lotId)
                        .eq(LotSamplingPlan::getRouteSeqNo, routeSeqNo)
                        .eq(LotSamplingPlan::getPassNo, passNo));
    }

    @Override
    public void save(LotSamplingPlan plan) {
        new DbRepositorySupport<>(mapper, LotSamplingPlan.class).save(plan);
    }

    @Override
    public void deleteAfter(Long lotId, Integer routeSeqNo) {
        mapper.delete(new LambdaQueryWrapper<LotSamplingPlan>()
                .eq(LotSamplingPlan::getLotId, lotId)
                .gt(LotSamplingPlan::getRouteSeqNo, routeSeqNo));
    }
}
