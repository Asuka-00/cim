package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Material;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MaterialMapper 物料 Mapper
 * <p>
 * 映射 mes_material，并统计换算对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MaterialMapper extends BatchBaseMapper<Material> {

    /**
     * 统计仍引用该物料的单位换算行数。
     *
     * @param id 物料主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_uom_conversion WHERE material_id = #{id})
              + (SELECT COUNT(*) FROM mes_bom_item WHERE material_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
