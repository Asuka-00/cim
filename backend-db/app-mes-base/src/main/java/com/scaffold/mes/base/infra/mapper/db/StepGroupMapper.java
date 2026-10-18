package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.StepGroup;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StepGroupMapper 站点组 Mapper
 * <p>
 * 映射 mes_step_group，并统计站点对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StepGroupMapper extends BatchBaseMapper<StepGroup> {

    /**
     * 统计仍引用该站点组的站点行数。
     *
     * @param id 站点组主键
     * @return 引用条数
     */
    @Select("SELECT COUNT(*) FROM mes_step WHERE step_group_id = #{id}")
    long countReferences(@Param("id") Long id);
}
