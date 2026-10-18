package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Customer;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * CustomerMapper 客户 Mapper
 * <p>
 * 映射 mes_customer，并统计产品对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface CustomerMapper extends BatchBaseMapper<Customer> {

    /**
     * 统计仍引用该客户的产品行数。
     *
     * @param id 客户主键
     * @return 引用条数
     */
    @Select("SELECT COUNT(*) FROM mes_product WHERE customer_id = #{id}")
    long countReferences(@Param("id") Long id);
}
