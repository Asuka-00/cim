package com.scaffold.mes.base.infra.repository.db;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ProductVersion;
import com.scaffold.mes.base.domain.repository.ProductVersionRepository;
import com.scaffold.mes.base.infra.mapper.db.ProductVersionMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ProductVersionDbRepositoryImpl 产品版本仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_product_version。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ProductVersionDbRepositoryImpl implements ProductVersionRepository {

    private final ProductVersionMapper mapper;

    private DbRepositorySupport<ProductVersion> support() {
        return new DbRepositorySupport<>(mapper, ProductVersion.class);
    }

    @Override
    public Optional<ProductVersion> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public PageDTO<ProductVersion> page(Long productId, int page, int pageSize) {
        return support().queryPage(page, pageSize, support().wrapper()
                .eq(ProductVersion::getProductId, productId)
                .orderByDesc(ProductVersion::getVersionNo));
    }

    @Override
    public List<ProductVersion> listActive(Long productId) {
        return mapper.selectList(support().wrapper()
                .eq(ProductVersion::getProductId, productId)
                .eq(ProductVersion::getStatus, "ACTIVE"));
    }

    @Override
    public List<ProductVersion> listActiveByRoute(Long routeId) {
        return mapper.selectList(support().wrapper()
                .eq(ProductVersion::getRouteId, routeId)
                .eq(ProductVersion::getStatus, "ACTIVE"));
    }

    @Override
    public int nextVersionNo(Long productId) {
        ProductVersion latest = mapper.selectOne(support().wrapper()
                .eq(ProductVersion::getProductId, productId)
                .orderByDesc(ProductVersion::getVersionNo)
                .last("LIMIT 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    @Override
    public void save(ProductVersion version) {
        support().save(version);
    }

    @Override
    public void update(ProductVersion version) {
        support().update(version);
    }
}
