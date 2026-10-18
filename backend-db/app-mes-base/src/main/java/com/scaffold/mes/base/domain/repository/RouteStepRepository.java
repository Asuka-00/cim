package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.RouteStep;

/**
 * RouteStepRepository Route 步骤仓储
 * <p>
 * 按当前租户读写 mes_route_step。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteStepRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 步骤，不存在时为空
     */
    Optional<RouteStep> findById(Long id);

    /**
     * 按版本和序号查询。
     *
     * @param routeVersionId Route 版本
     * @param seqNo 步骤序号
     * @return 步骤，不存在时为空
     */
    Optional<RouteStep> findBySeq(Long routeVersionId, Integer seqNo);

    /**
     * 列出某个版本的全部步骤，按序号从小到大。
     *
     * @param routeVersionId Route 版本
     * @return 步骤列表
     */
    List<RouteStep> listByVersion(Long routeVersionId);

    /**
     * 插入。
     *
     * @param step 步骤
     */
    void save(RouteStep step);

    /**
     * 更新。
     *
     * @param step 已加载的步骤
     */
    void update(RouteStep step);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计步骤级 Recipe 覆盖行数。
     *
     * @param id Route 步骤主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
