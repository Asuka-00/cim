package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.StepSampling;

/**
 * StepSamplingRepository 站点抽检绑定仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface StepSamplingRepository {

    /**
     * 按站点与产品查询。
     *
     * @param stepId 量测站点
     * @param productId 产品。0 表示通用
     * @return 绑定，不存在时为空
     */
    Optional<StepSampling> find(Long stepId, Long productId);

    /**
     * 列出全部绑定。
     *
     * @return 按站点排列
     */
    List<StepSampling> list();

    /**
     * 插入。
     *
     * @param binding 绑定
     */
    void save(StepSampling binding);
}
