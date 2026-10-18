package com.scaffold.mes.base.domain.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ProductVersion;

/**
 * ProductVersionRepository 产品版本仓储
 * <p>
 * 按当前租户读写 mes_product_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ProductVersionRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 版本，不存在时为空
     */
    Optional<ProductVersion> findById(Long id);

    /**
     * 分页查询某个产品的版本。
     *
     * @param productId 产品
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 按版本号倒序的分页结果
     */
    PageDTO<ProductVersion> page(Long productId, int page, int pageSize);

    /**
     * 列出某个产品当前生效的版本。
     *
     * @param productId 产品
     * @return 状态为 ACTIVE 的版本
     */
    List<ProductVersion> listActive(Long productId);

    /**
     * 列出绑定某条 Route 且当前生效的产品版本。
     *
     * @param routeId Route 主数据
     * @return 状态为 ACTIVE 的产品版本
     */
    List<ProductVersion> listActiveByRoute(Long routeId);

    /**
     * 计算下一个版本号。
     *
     * @param productId 产品
     * @return 现有最大版本号加 1，没有版本时为 1
     */
    int nextVersionNo(Long productId);

    /**
     * 插入。
     *
     * @param version 版本
     */
    void save(ProductVersion version);

    /**
     * 更新。
     *
     * @param version 已加载的版本
     */
    void update(ProductVersion version);
}
