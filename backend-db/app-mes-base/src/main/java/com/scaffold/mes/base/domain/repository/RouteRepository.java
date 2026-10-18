package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Route;

/**
 * RouteRepository Route 仓储
 * <p>
 * 按当前租户读写 mes_route。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface RouteRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return Route，不存在时为空
     */
    Optional<Route> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param routeCode Route 编码
     * @return Route，不存在时为空
     */
    Optional<Route> findByCode(String routeCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param routeCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Route> page(int page, int pageSize, String routeCode);

    /**
     * 插入。
     *
     * @param route Route
     */
    void save(Route route);

    /**
     * 更新。
     *
     * @param route 已加载的 Route
     */
    void update(Route route);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计版本引用数。
     * <p>
     * 只统计 mes_route_version。产品版本绑定 route_id 后要补进这条统计。
     *
     * @param id Route 主键
     * @return 引用条数
     */
    long countReferences(Long id);

    /**
     * 锁住 Route 主表行，供版本生效串行化。
     *
     * @param id Route 主键
     * @throws com.scaffold.share.exception.BusinessException Route 不存在或行被占用
     */
    void lock(Long id);
}
