package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Product;

/**
 * ProductRepository 产品仓储
 * <p>
 * 按当前租户读写 mes_product。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ProductRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 产品，不存在时为空
     */
    Optional<Product> findById(Long id);

    /**
     * 按料号查询。
     *
     * @param productCode 产品料号
     * @return 产品，不存在时为空
     */
    Optional<Product> findByCode(String productCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param productCode 料号模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Product> page(int page, int pageSize, String productCode);

    /**
     * 插入。
     *
     * @param product 产品
     */
    void save(Product product);

    /**
     * 更新。
     *
     * @param product 已加载的产品
     */
    void update(Product product);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计换算、产品版本、站点 Recipe 和设备能力对产品的引用。
     *
     * @param id 产品主键
     * @return 引用条数
     */
    long countReferences(Long id);

    /**
     * 锁住产品主表行，供版本生效串行化。
     *
     * @param id 产品主键
     * @throws com.scaffold.share.exception.BusinessException 产品不存在或行被占用
     */
    void lock(Long id);
}
