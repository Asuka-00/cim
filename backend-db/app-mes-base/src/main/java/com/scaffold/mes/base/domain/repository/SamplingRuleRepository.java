package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.SamplingRule;

/**
 * SamplingRuleRepository 抽检规则仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface SamplingRuleRepository {

    /**
     * 按编码查询。
     *
     * @param ruleCode 规则编码
     * @return 规则，不存在时为空
     */
    Optional<SamplingRule> findByCode(String ruleCode);

    /**
     * 按主键查询。
     *
     * @param id 规则
     * @return 规则，不存在时为空
     */
    Optional<SamplingRule> findById(Long id);

    /**
     * 列出全部规则，按编码。
     *
     * @return 规则
     */
    List<SamplingRule> list();

    /**
     * 插入。
     *
     * @param rule 规则
     */
    void save(SamplingRule rule);
}
