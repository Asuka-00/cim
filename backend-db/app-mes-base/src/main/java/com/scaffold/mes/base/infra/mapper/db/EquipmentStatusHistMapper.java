package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.scaffold.mes.base.domain.entity.EquipmentStatusHist;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * EquipmentStatusHistMapper 设备状态历史 Mapper
 * <p>
 * 映射 hist_mes_equipment_status。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface EquipmentStatusHistMapper extends BatchBaseMapper<EquipmentStatusHist> {

    /**
     * 删除某设备尚未结束、且没有变更前状态的初始段。
     *
     * @param equipmentId 设备
     * @return 删除行数
     */
    @Delete("""
            DELETE FROM hist_mes_equipment_status
            WHERE equipment_id = #{equipmentId}
              AND end_at IS NULL
              AND from_e10_state IS NULL
            """)
    int deleteUntouchedInitial(@Param("equipmentId") Long equipmentId);
}
