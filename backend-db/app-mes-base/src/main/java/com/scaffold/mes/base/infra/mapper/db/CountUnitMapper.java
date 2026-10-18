package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * CountUnitMapper 计数单位 Mapper
 * <p>
 * 映射 mes_count_unit，并统计产品、换算对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface CountUnitMapper extends BatchBaseMapper<CountUnit> {

    /**
     * 统计仍引用该计数单位的产品与换算行数。
     *
     * @param id 计数单位主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_product WHERE base_unit_id = #{id})
              + (SELECT COUNT(*) FROM mes_product_unit_conv
                 WHERE from_unit_id = #{id} OR to_unit_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
