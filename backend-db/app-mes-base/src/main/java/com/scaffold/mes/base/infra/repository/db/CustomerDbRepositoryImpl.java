package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Customer;
import com.scaffold.mes.base.domain.repository.CustomerRepository;
import com.scaffold.mes.base.infra.mapper.db.CustomerMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * CustomerDbRepositoryImpl 客户仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_customer。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class CustomerDbRepositoryImpl implements CustomerRepository {

    private final CustomerMapper mapper;

    private DbRepositorySupport<Customer> support() {
        return new DbRepositorySupport<>(mapper, Customer.class);
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Customer> findByCode(String customerCode) {
        return support().findOne(new LambdaQueryWrapper<Customer>().eq(Customer::getCustomerCode, customerCode));
    }

    @Override
    public PageDTO<Customer> page(int page, int pageSize, String customerCode) {
        LambdaQueryWrapper<Customer> wrapper = support().wrapper();
        if (customerCode != null && !customerCode.isBlank()) {
            wrapper.like(Customer::getCustomerCode, customerCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Customer customer) {
        support().save(customer);
    }

    @Override
    public void update(Customer customer) {
        support().update(customer);
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
