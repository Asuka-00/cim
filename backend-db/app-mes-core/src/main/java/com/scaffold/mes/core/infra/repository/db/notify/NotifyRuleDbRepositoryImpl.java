package com.scaffold.mes.core.infra.repository.db.notify;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.notify.NotifyRule;
import com.scaffold.mes.core.domain.repository.notify.NotifyRuleRepository;
import com.scaffold.mes.core.infra.mapper.db.notify.NotifyRuleMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * NotifyRuleDbRepositoryImpl 通知规则仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class NotifyRuleDbRepositoryImpl implements NotifyRuleRepository {

    private final NotifyRuleMapper mapper;

    @Override
    public Optional<NotifyRule> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<NotifyRule> findByCode(String ruleCode) {
        return new DbRepositorySupport<>(mapper, NotifyRule.class)
                .findOne(new LambdaQueryWrapper<NotifyRule>().eq(NotifyRule::getRuleCode, ruleCode));
    }

    @Override
    public List<NotifyRule> listEnabledByEvent(String eventType) {
        return new DbRepositorySupport<>(mapper, NotifyRule.class).queryAll(
                new LambdaQueryWrapper<NotifyRule>()
                        .eq(NotifyRule::getEventType, eventType)
                        .eq(NotifyRule::getEnabled, 1));
    }

    @Override
    public void save(NotifyRule rule) {
        new DbRepositorySupport<>(mapper, NotifyRule.class).save(rule);
    }

    @Override
    public void update(NotifyRule rule) {
        new DbRepositorySupport<>(mapper, NotifyRule.class).update(rule);
    }
}
