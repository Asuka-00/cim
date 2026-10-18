package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.SamplingCounter;

/**
 * SamplingCounterRepository 抽检计数器仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface SamplingCounterRepository {

    /**
     * 按规则、站点、步骤和产品查询。
     *
     * @param ruleId 规则
     * @param stepId 站点
     * @param routeStepId Route 步骤。0 表示不区分
     * @param productId 产品。0 表示通用
     * @return 计数器，不存在时为空
     */
    Optional<SamplingCounter> find(Long ruleId, Long stepId, Long routeStepId, Long productId);

    /**
     * 列出全部计数器。
     *
     * @return 按规则排列
     */
    List<SamplingCounter> list();

    /**
     * 插入。
     *
     * @param counter 计数器
     */
    void save(SamplingCounter counter);

    /**
     * 更新计数。
     *
     * @param counter 计数器
     */
    void update(SamplingCounter counter);
}
