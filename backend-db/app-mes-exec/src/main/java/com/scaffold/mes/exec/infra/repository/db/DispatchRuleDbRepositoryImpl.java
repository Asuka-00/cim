package com.scaffold.mes.exec.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.exec.domain.entity.DispatchRule;
import com.scaffold.mes.exec.domain.repository.DispatchRuleRepository;
import com.scaffold.mes.exec.infra.mapper.db.DispatchRuleMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * DispatchRuleDbRepositoryImpl 排序规则仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class DispatchRuleDbRepositoryImpl implements DispatchRuleRepository {

    private final DispatchRuleMapper mapper;

    private DbRepositorySupport<DispatchRule> support() {
        return new DbRepositorySupport<>(mapper, DispatchRule.class);
    }

    @Override
    public Optional<DispatchRule> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<DispatchRule> findByCode(String ruleCode) {
        return support().findOne(new LambdaQueryWrapper<DispatchRule>().eq(DispatchRule::getRuleCode, ruleCode));
    }

    @Override
    public Optional<DispatchRule> findDefault() {
        return support().findOne(new LambdaQueryWrapper<DispatchRule>()
                .eq(DispatchRule::getIsDefault, 1)
                .eq(DispatchRule::getEnabled, 1));
    }

    @Override
    public List<DispatchRule> list() {
        return mapper.selectList(new LambdaQueryWrapper<DispatchRule>().orderByAsc(DispatchRule::getRuleCode));
    }

    @Override
    public void save(DispatchRule rule) {
        support().save(rule);
    }
}
