package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.RouteTimeLink;

/**
 * RouteTimeLinkRepository Time Link 仓储
 * <p>
 * 按当前租户读写 mes_route_time_link。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteTimeLinkRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return Time Link，不存在时为空
     */
    Optional<RouteTimeLink> findById(Long id);

    /**
     * 按版本和起止序号查询。
     *
     * @param routeVersionId Route 版本
     * @param fromSeqNo 起点序号
     * @param toSeqNo 终点序号
     * @return Time Link，不存在时为空
     */
    Optional<RouteTimeLink> findExact(Long routeVersionId, Integer fromSeqNo, Integer toSeqNo);

    /**
     * 列出某个版本的 Time Link。
     *
     * @param routeVersionId Route 版本
     * @return 按起点序号排序的列表
     */
    List<RouteTimeLink> listByVersion(Long routeVersionId);

    /**
     * 插入。
     *
     * @param link Time Link
     */
    void save(RouteTimeLink link);

    /**
     * 更新。
     *
     * @param link 已加载的 Time Link
     */
    void update(RouteTimeLink link);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
