package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * UomMapper 物料计量单位 Mapper
 * <p>
 * 映射 mes_uom，并统计换算、物料对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface UomMapper extends BatchBaseMapper<Uom> {

    /**
     * 统计仍引用该计量单位的换算与物料行数。
     *
     * @param id 计量单位主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_uom_conversion
                 WHERE from_uom_id = #{id} OR to_uom_id = #{id})
              + (SELECT COUNT(*) FROM mes_material WHERE base_uom_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
