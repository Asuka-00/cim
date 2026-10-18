package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.DieBinLog;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * DieBinLogMapper Die Bin 流水 Mapper
 * <p>
 * 映射 hist_mes_die_bin_log。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface DieBinLogMapper extends BatchBaseMapper<DieBinLog> {
}
