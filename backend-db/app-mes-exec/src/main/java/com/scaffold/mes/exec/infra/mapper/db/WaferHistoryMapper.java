package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * WaferHistoryMapper Wafer 历史 Mapper
 * <p>
 * 映射 hist_mes_wafer_history。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface WaferHistoryMapper extends BatchBaseMapper<WaferHistory> {
}
