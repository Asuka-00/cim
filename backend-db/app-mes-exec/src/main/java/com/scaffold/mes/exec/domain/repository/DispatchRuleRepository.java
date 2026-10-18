package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.DispatchRule;

/**
 * DispatchRuleRepository 排序规则仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface DispatchRuleRepository {

    /**
     * 按主键查询。
     *
     * @param id 规则
     * @return 规则，不存在时为空
     */
    Optional<DispatchRule> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param ruleCode 编码
     * @return 规则，不存在时为空
     */
    Optional<DispatchRule> findByCode(String ruleCode);

    /**
     * 取启用的默认规则。
     *
     * @return 默认规则，没有时为空
     */
    Optional<DispatchRule> findDefault();

    /**
     * 列出全部规则，按编码。
     *
     * @return 规则
     */
    List<DispatchRule> list();

    /**
     * 插入。
     *
     * @param rule 规则
     */
    void save(DispatchRule rule);
}
