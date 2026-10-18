package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.StepSampling;
import com.scaffold.mes.base.domain.repository.StepSamplingRepository;
import com.scaffold.mes.base.infra.mapper.db.StepSamplingMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * StepSamplingDbRepositoryImpl 站点抽检绑定仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class StepSamplingDbRepositoryImpl implements StepSamplingRepository {

    private final StepSamplingMapper mapper;

    @Override
    public Optional<StepSampling> find(Long stepId, Long productId) {
        return new DbRepositorySupport<>(mapper, StepSampling.class)
                .findOne(new LambdaQueryWrapper<StepSampling>()
                        .eq(StepSampling::getStepId, stepId)
                        .eq(StepSampling::getProductId, productId));
    }

    @Override
    public List<StepSampling> list() {
        return mapper.selectList(new LambdaQueryWrapper<StepSampling>().orderByAsc(StepSampling::getStepId));
    }

    @Override
    public void save(StepSampling binding) {
        new DbRepositorySupport<>(mapper, StepSampling.class).save(binding);
    }
}
