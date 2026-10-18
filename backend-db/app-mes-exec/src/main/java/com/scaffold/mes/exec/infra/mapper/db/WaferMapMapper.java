package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.WaferMap;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * WaferMapMapper Wafer Map Mapper
 * <p>
 * 映射 mes_wafer_map。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface WaferMapMapper extends BatchBaseMapper<WaferMap> {
}
