package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ProductUnitConv;
import com.scaffold.mes.base.domain.repository.ProductUnitConvRepository;
import com.scaffold.mes.base.infra.mapper.db.ProductUnitConvMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ProductUnitConvDbRepositoryImpl 产品计数换算仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_product_unit_conv。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ProductUnitConvDbRepositoryImpl implements ProductUnitConvRepository {

    private final ProductUnitConvMapper mapper;

    private DbRepositorySupport<ProductUnitConv> support() {
        return new DbRepositorySupport<>(mapper, ProductUnitConv.class);
    }

    @Override
    public Optional<ProductUnitConv> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<ProductUnitConv> findExact(Long productId, Long fromUnitId, Long toUnitId) {
        return support().findOne(new LambdaQueryWrapper<ProductUnitConv>()
                .eq(ProductUnitConv::getProductId, productId)
                .eq(ProductUnitConv::getFromUnitId, fromUnitId)
                .eq(ProductUnitConv::getToUnitId, toUnitId));
    }

    @Override
    public PageDTO<ProductUnitConv> page(int page, int pageSize, Long productId) {
        return support().queryPage(page, pageSize,
                support().wrapper().eq(ProductUnitConv::getProductId, productId));
    }

    @Override
    public void save(ProductUnitConv conversion) {
        support().save(conversion);
    }

    @Override
    public void update(ProductUnitConv conversion) {
        support().update(conversion);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }
}
