package com.scaffold.mes.core.infra.mapper.db.imports;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.imports.ImportTask;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ImportTaskMapper 导入任务 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ImportTaskMapper extends BatchBaseMapper<ImportTask> {
}
