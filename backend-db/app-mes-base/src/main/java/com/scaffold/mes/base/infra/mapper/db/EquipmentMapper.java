package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * EquipmentMapper 设备台账 Mapper
 * <p>
 * 映射 mes_equipment，并统计状态历史对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface EquipmentMapper extends BatchBaseMapper<Equipment> {

    /**
     * 统计已经发生过切换的状态历史。
     * <p>
     * 新建时那一段未结束、且没有变更前状态的记录不算引用，否则设备创建后就删不掉。
     *
     * @param id 设备主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM hist_mes_equipment_status
                WHERE equipment_id = #{id}
                  AND (end_at IS NOT NULL OR from_e10_state IS NOT NULL))
              + (SELECT COUNT(*) FROM mes_equipment_port WHERE equipment_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
