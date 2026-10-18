package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.ConfirmRule;

/**
 * ConfirmRuleRepository 二次确认规则仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ConfirmRuleRepository {

    /**
     * 按动作编码查询。
     *
     * @param actionCode 动作
     * @return 规则，不存在时为空
     */
    Optional<ConfirmRule> findByAction(String actionCode);

    /**
     * 列出全部规则，按动作编码。
     *
     * @return 规则
     */
    List<ConfirmRule> list();

    /**
     * 插入。
     *
     * @param rule 规则
     */
    void save(ConfirmRule rule);
}
