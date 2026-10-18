package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * WaferMapper Wafer Mapper
 * <p>
 * 映射 mes_wafer。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface WaferMapper extends BatchBaseMapper<Wafer> {
}
