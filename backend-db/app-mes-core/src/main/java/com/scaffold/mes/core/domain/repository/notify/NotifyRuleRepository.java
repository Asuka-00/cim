package com.scaffold.mes.core.domain.repository.notify;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.notify.NotifyRule;

/**
 * NotifyRuleRepository 通知规则仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface NotifyRuleRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 规则，不存在时为空
     */
    Optional<NotifyRule> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param ruleCode 规则编码
     * @return 规则，不存在时为空
     */
    Optional<NotifyRule> findByCode(String ruleCode);

    /**
     * 按事件类型列出已启用的规则。
     *
     * @param eventType 事件类型
     * @return 启用中的规则。没有时为空列表
     */
    List<NotifyRule> listEnabledByEvent(String eventType);

    /**
     * 插入。
     *
     * @param rule 规则
     */
    void save(NotifyRule rule);

    /**
     * 更新。
     *
     * @param rule 规则
     */
    void update(NotifyRule rule);
}
