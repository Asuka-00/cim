package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.ProductFamily;
import com.scaffold.mes.base.domain.repository.ProductFamilyRepository;
import com.scaffold.mes.base.infra.mapper.db.ProductFamilyMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ProductFamilyDbRepositoryImpl 产品族仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_product_family。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ProductFamilyDbRepositoryImpl implements ProductFamilyRepository {

    private final ProductFamilyMapper mapper;

    private DbRepositorySupport<ProductFamily> support() {
        return new DbRepositorySupport<>(mapper, ProductFamily.class);
    }

    @Override
    public Optional<ProductFamily> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<ProductFamily> findByCode(String familyCode) {
        return support().findOne(new LambdaQueryWrapper<ProductFamily>().eq(ProductFamily::getFamilyCode, familyCode));
    }

    @Override
    public PageDTO<ProductFamily> page(int page, int pageSize, String familyCode) {
        LambdaQueryWrapper<ProductFamily> wrapper = support().wrapper();
        if (familyCode != null && !familyCode.isBlank()) {
            wrapper.like(ProductFamily::getFamilyCode, familyCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(ProductFamily family) {
        support().save(family);
    }

    @Override
    public void update(ProductFamily family) {
        support().update(family);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }
}
