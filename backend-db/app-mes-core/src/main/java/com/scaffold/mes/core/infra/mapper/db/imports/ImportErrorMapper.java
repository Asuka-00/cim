package com.scaffold.mes.core.infra.mapper.db.imports;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.imports.ImportError;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ImportErrorMapper 导入错误明细 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ImportErrorMapper extends BatchBaseMapper<ImportError> {
}
