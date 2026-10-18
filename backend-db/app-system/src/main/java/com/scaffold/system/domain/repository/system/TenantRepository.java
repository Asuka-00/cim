package com.scaffold.system.domain.repository.system;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.system.domain.entity.system.Tenant;
import com.scaffold.system.domain.query.system.TenantQuery;

/**
 * TenantRepository仓储接口
 * <p>
 * 负责定义TenantRepository相关数据访问能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public interface TenantRepository {

    /**
     * 根据ID查询租户
     */
    Optional<Tenant> findById(Long id);

    /**
     * 根据租户编码查询租户
     */
    Optional<Tenant> findByTenantId(String tenantId);

    /**
     * 查询所有租户
     */
    List<Tenant> findAll();

    /**
     * 查询当前有效记录：enabled==ENABLED。
     */
    List<Tenant> findActiveAll();

    /**
     * 保存租户
     */
    void save(Tenant tenant);

    /**
     * 更新租户
     */
    void update(Tenant tenant);

    /**
     * 根据ID删除租户
     */
    void deleteById(Long id);

    /**
     * 分页查询租户
     *
     * @param page     当前页码
     * @param pageSize 每页大小
     * @param query    查询条件
     * @return 分页结果
     */
    PageDTO<Tenant> queryTenantPage(int page, int pageSize, TenantQuery query);
}
