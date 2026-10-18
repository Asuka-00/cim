package com.scaffold.mes.core.infra.mapper.db.print;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.print.PrintLog;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * PrintLogMapper 打印记录 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface PrintLogMapper extends BatchBaseMapper<PrintLog> {
}
