package com.scaffold.mes.core.infra.repository.db.sequence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.sequence.SequenceRule;
import com.scaffold.mes.core.domain.repository.sequence.SequenceRuleRepository;
import com.scaffold.mes.core.infra.mapper.db.sequence.SequenceRuleMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * SequenceRuleDbRepositoryImpl 编号规则仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_sequence_rule。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class SequenceRuleDbRepositoryImpl implements SequenceRuleRepository {

    private final SequenceRuleMapper mapper;

    private DbRepositorySupport<SequenceRule> support() {
        return new DbRepositorySupport<>(mapper, SequenceRule.class);
    }

    @Override
    public Optional<SequenceRule> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<SequenceRule> findByRuleCode(String ruleCode) {
        return support().findOne(new LambdaQueryWrapper<SequenceRule>().eq(SequenceRule::getRuleCode, ruleCode));
    }

    @Override
    public PageDTO<SequenceRule> page(int page, int pageSize, String ruleCode) {
        LambdaQueryWrapper<SequenceRule> wrapper = support().wrapper();
        if (ruleCode != null && !ruleCode.isBlank()) {
            wrapper.like(SequenceRule::getRuleCode, ruleCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(SequenceRule rule) {
        support().save(rule);
    }

    @Override
    public void update(SequenceRule rule) {
        support().updateWithVersion(rule);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
