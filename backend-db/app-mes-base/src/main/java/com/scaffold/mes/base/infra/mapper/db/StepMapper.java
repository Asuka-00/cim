package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StepMapper 站点 Mapper
 * <p>
 * 映射 mes_step。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StepMapper extends BatchBaseMapper<Step> {

    /**
     * 统计站点设备组和站点 Recipe 绑定行数。
     *
     * @param id 站点主键
     * @return 引用条数
     */
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_step_equipment_group WHERE step_id = #{id})
              + (SELECT COUNT(*) FROM mes_step_recipe WHERE step_id = #{id})
            """)
    long countReferences(@Param("id") Long id);
}
