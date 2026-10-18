package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ProductUnitConv;

/**
 * ProductUnitConvRepository 产品计数换算仓储
 * <p>
 * 按当前租户读写 mes_product_unit_conv。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ProductUnitConvRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 换算，不存在时为空
     */
    Optional<ProductUnitConv> findById(Long id);

    /**
     * 按产品与两个计数单位精确查询。
     *
     * @param productId 产品
     * @param fromUnitId 源计数单位
     * @param toUnitId 目标计数单位
     * @return 换算，不存在时为空
     */
    Optional<ProductUnitConv> findExact(Long productId, Long fromUnitId, Long toUnitId);

    /**
     * 分页查询某个产品的换算。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param productId 产品
     * @return 分页结果
     */
    PageDTO<ProductUnitConv> page(int page, int pageSize, Long productId);

    /**
     * 插入。
     *
     * @param conversion 换算
     */
    void save(ProductUnitConv conversion);

    /**
     * 更新。
     *
     * @param conversion 已加载的换算
     */
    void update(ProductUnitConv conversion);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
