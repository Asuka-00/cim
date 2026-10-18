package com.scaffold.mes.base.infra.repository.db;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.infra.mapper.db.ProductMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ProductDbRepositoryImpl 产品仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_product。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ProductDbRepositoryImpl implements ProductRepository {

    private final ProductMapper mapper;

    private DbRepositorySupport<Product> support() {
        return new DbRepositorySupport<>(mapper, Product.class);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Product> findByCode(String productCode) {
        return support().findOne(new LambdaQueryWrapper<Product>().eq(Product::getProductCode, productCode));
    }

    @Override
    public PageDTO<Product> page(int page, int pageSize, String productCode) {
        LambdaQueryWrapper<Product> wrapper = support().wrapper();
        if (productCode != null && !productCode.isBlank()) {
            wrapper.like(Product::getProductCode, productCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Product product) {
        support().save(product);
    }

    @Override
    public void update(Product product) {
        support().update(product);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }

    @Override
    public void lock(Long id) {
        try {
            Product locked = mapper.selectOne(new LambdaQueryWrapper<Product>().eq(Product::getId, id).last("FOR UPDATE NOWAIT"));
            if (locked == null) {
                throw new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            Throwable current = ex;
            while (current != null) {
                if (current instanceof SQLException sql && sql.getErrorCode() == 3572) {
                    throw new BusinessException(400102, "对象被占用");
                }
                if (current instanceof SQLException sql && sql.getErrorCode() == 1205) {
                    throw new BusinessException(400103, "锁等待超时");
                }
                current = current.getCause();
            }
            throw ex;
        }
    }
}
