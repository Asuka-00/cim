package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.SamplingCounter;
import com.scaffold.mes.exec.domain.repository.SamplingCounterRepository;
import com.scaffold.mes.exec.infra.mapper.db.SamplingCounterMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * SamplingCounterDbRepositoryImpl 抽检计数器仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class SamplingCounterDbRepositoryImpl implements SamplingCounterRepository {

    private final SamplingCounterMapper mapper;

    @Override
    public Optional<SamplingCounter> find(Long ruleId, Long stepId, Long routeStepId, Long productId) {
        return new DbRepositorySupport<>(mapper, SamplingCounter.class)
                .findOne(new LambdaQueryWrapper<SamplingCounter>()
                        .eq(SamplingCounter::getRuleId, ruleId)
                        .eq(SamplingCounter::getStepId, stepId)
                        .eq(SamplingCounter::getRouteStepId, routeStepId)
                        .eq(SamplingCounter::getProductId, productId));
    }

    @Override
    public List<SamplingCounter> list() {
        return mapper.selectList(new LambdaQueryWrapper<SamplingCounter>().orderByAsc(SamplingCounter::getRuleId));
    }

    @Override
    public void save(SamplingCounter counter) {
        new DbRepositorySupport<>(mapper, SamplingCounter.class).save(counter);
    }

    @Override
    public void update(SamplingCounter counter) {
        new DbRepositorySupport<>(mapper, SamplingCounter.class).update(counter);
    }
}
