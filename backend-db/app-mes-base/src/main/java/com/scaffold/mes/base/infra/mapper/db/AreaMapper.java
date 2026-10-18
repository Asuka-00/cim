package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Area;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * AreaMapper 区域 Mapper
 * <p>
 * 映射 mes_area，并统计下级区域、站点和设备对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface AreaMapper extends BatchBaseMapper<Area> {

    /**
     * 统计仍引用该区域的下级区域、站点、设备组和设备行数。
     *
     * @param id 区域主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_area WHERE parent_id = #{id})
              + (SELECT COUNT(*) FROM mes_step WHERE area_id = #{id})
              + (SELECT COUNT(*) FROM mes_equipment_group WHERE area_id = #{id})
              + (SELECT COUNT(*) FROM mes_equipment WHERE area_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
