package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.ProductFamily;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ProductFamilyMapper 产品族 Mapper
 * <p>
 * 映射 mes_product_family，并统计产品对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ProductFamilyMapper extends BatchBaseMapper<ProductFamily> {

    /**
     * 统计仍引用该产品族的产品行数。
     *
     * @param id 产品族主键
     * @return 引用条数
     */
    @Select("SELECT COUNT(*) FROM mes_product WHERE family_id = #{id}")
    long countReferences(@Param("id") Long id);
}
