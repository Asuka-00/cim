package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.EquipmentGroup;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * EquipmentGroupMapper 设备组 Mapper
 * <p>
 * 映射 mes_equipment_group，并统计设备对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface EquipmentGroupMapper extends BatchBaseMapper<EquipmentGroup> {

    /**
     * 统计仍引用该设备组的设备和站点绑定行数。
     *
     * @param id 设备组主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_equipment WHERE group_id = #{id})
              + (SELECT COUNT(*) FROM mes_step_equipment_group WHERE group_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
