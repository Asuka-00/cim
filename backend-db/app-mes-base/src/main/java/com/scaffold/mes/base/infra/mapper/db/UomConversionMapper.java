package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.UomConversion;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * UomConversionMapper 物料单位换算 Mapper
 * <p>
 * 映射 mes_uom_conversion。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface UomConversionMapper extends BatchBaseMapper<UomConversion> {
}
