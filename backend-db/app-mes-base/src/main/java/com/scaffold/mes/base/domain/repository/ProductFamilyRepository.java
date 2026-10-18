package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ProductFamily;

/**
 * ProductFamilyRepository 产品族仓储
 * <p>
 * 按当前租户读写 mes_product_family。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ProductFamilyRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 产品族，不存在时为空
     */
    Optional<ProductFamily> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param familyCode 产品族编码
     * @return 产品族，不存在时为空
     */
    Optional<ProductFamily> findByCode(String familyCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param familyCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<ProductFamily> page(int page, int pageSize, String familyCode);

    /**
     * 插入。
     *
     * @param family 产品族
     */
    void save(ProductFamily family);

    /**
     * 更新。
     *
     * @param family 已加载的产品族
     */
    void update(ProductFamily family);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计产品引用数。
     *
     * @param id 产品族主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
