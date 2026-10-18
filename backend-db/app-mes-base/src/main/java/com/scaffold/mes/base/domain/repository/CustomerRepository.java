package com.scaffold.mes.base.domain.repository;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Customer;

/**
 * CustomerRepository 客户仓储
 * <p>
 * 按当前租户读写 mes_customer。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface CustomerRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 客户，不存在时为空
     */
    Optional<Customer> findById(Long id);

    /**
     * 按编码查询。
     *
     * @param customerCode 客户编码
     * @return 客户，不存在时为空
     */
    Optional<Customer> findByCode(String customerCode);

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param customerCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    PageDTO<Customer> page(int page, int pageSize, String customerCode);

    /**
     * 插入。
     *
     * @param customer 客户
     */
    void save(Customer customer);

    /**
     * 更新。
     *
     * @param customer 已加载的客户
     */
    void update(Customer customer);

    /**
     * 物理删除。
     *
     * @param id 主键
     */
    void deleteById(Long id);

    /**
     * 统计产品引用数。
     * <p>
     * 只统计 M1 已建表。后续里程碑新增 customer_id 列时要补进这条统计。
     *
     * @param id 客户主键
     * @return 引用条数
     */
    long countReferences(Long id);
}
