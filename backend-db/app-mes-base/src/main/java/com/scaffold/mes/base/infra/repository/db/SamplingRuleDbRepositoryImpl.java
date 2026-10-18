package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.SamplingRule;
import com.scaffold.mes.base.domain.repository.SamplingRuleRepository;
import com.scaffold.mes.base.infra.mapper.db.SamplingRuleMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * SamplingRuleDbRepositoryImpl 抽检规则仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class SamplingRuleDbRepositoryImpl implements SamplingRuleRepository {

    private final SamplingRuleMapper mapper;

    @Override
    public Optional<SamplingRule> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<SamplingRule> findByCode(String ruleCode) {
        return new DbRepositorySupport<>(mapper, SamplingRule.class)
                .findOne(new LambdaQueryWrapper<SamplingRule>().eq(SamplingRule::getRuleCode, ruleCode));
    }

    @Override
    public List<SamplingRule> list() {
        return mapper.selectList(new LambdaQueryWrapper<SamplingRule>().orderByAsc(SamplingRule::getRuleCode));
    }

    @Override
    public void save(SamplingRule rule) {
        new DbRepositorySupport<>(mapper, SamplingRule.class).save(rule);
    }
}
