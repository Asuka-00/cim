package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.UnitConversion;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * UnitConversionMapper 单位转换历史 Mapper
 * <p>
 * 映射 hist_mes_unit_conversion。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface UnitConversionMapper extends BatchBaseMapper<UnitConversion> {
}
