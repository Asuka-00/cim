package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.ConfirmRule;
import com.scaffold.mes.base.domain.repository.ConfirmRuleRepository;
import com.scaffold.mes.base.infra.mapper.db.ConfirmRuleMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ConfirmRuleDbRepositoryImpl 二次确认规则仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ConfirmRuleDbRepositoryImpl implements ConfirmRuleRepository {

    private final ConfirmRuleMapper mapper;

    @Override
    public Optional<ConfirmRule> findByAction(String actionCode) {
        return new DbRepositorySupport<>(mapper, ConfirmRule.class)
                .findOne(new LambdaQueryWrapper<ConfirmRule>().eq(ConfirmRule::getActionCode, actionCode));
    }

    @Override
    public List<ConfirmRule> list() {
        return mapper.selectList(new LambdaQueryWrapper<ConfirmRule>().orderByAsc(ConfirmRule::getActionCode));
    }

    @Override
    public void save(ConfirmRule rule) {
        new DbRepositorySupport<>(mapper, ConfirmRule.class).save(rule);
    }
}
