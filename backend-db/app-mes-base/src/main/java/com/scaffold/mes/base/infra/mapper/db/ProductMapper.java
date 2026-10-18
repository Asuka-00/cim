package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ProductMapper 产品 Mapper
 * <p>
 * 映射 mes_product，并统计换算对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ProductMapper extends BatchBaseMapper<Product> {

    /**
     * 统计仍引用该产品的计数换算行数。
     *
     * @param id 产品主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_product_unit_conv WHERE product_id = #{id})
              + (SELECT COUNT(*) FROM mes_product_version WHERE product_id = #{id})
              + (SELECT COUNT(*) FROM mes_step_recipe WHERE product_id = #{id})
              + (SELECT COUNT(*) FROM mes_route_step_recipe WHERE product_id = #{id})
              + (SELECT COUNT(*) FROM mes_equipment_capability WHERE product_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
