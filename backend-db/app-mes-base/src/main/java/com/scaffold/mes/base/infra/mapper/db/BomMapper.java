package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.scaffold.mes.base.domain.entity.Bom;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * BomMapper BOM Mapper
 * <p>
 * 映射 mes_bom，并统计版本对它的引用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface BomMapper extends BatchBaseMapper<Bom> {

    /**
     * 统计仍引用该 BOM 的版本行数。
     *
     * @param id BOM 主键
     * @return 引用条数
     */
    @Select("SELECT COUNT(*) FROM mes_bom_version WHERE bom_id = #{id}")
    long countReferences(@Param("id") Long id);
}
