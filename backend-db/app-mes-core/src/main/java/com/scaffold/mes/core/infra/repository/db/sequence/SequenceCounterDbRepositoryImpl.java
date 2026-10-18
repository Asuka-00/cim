package com.scaffold.mes.core.infra.repository.db.sequence;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.sequence.SequenceCounter;
import com.scaffold.mes.core.domain.repository.sequence.SequenceCounterRepository;
import com.scaffold.mes.core.infra.mapper.db.sequence.SequenceCounterMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * SequenceCounterDbRepositoryImpl 编号流水仓储实现
 * <p>
 * 锁定计数行使用 FOR UPDATE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class SequenceCounterDbRepositoryImpl implements SequenceCounterRepository {

    private final SequenceCounterMapper mapper;

    @Override
    public SequenceCounter lock(Long ruleId, String periodKey) {
        return mapper.selectOne(new LambdaQueryWrapper<SequenceCounter>()
                .eq(SequenceCounter::getRuleId, ruleId)
                .eq(SequenceCounter::getPeriodKey, periodKey)
                .last("FOR UPDATE"));
    }

    @Override
    public void save(SequenceCounter counter) {
        new DbRepositorySupport<>(mapper, SequenceCounter.class).save(counter);
    }

    @Override
    public void update(SequenceCounter counter) {
        mapper.updateById(counter);
    }
}
