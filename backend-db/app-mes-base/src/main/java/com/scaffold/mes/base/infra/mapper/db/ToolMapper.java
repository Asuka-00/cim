package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.Tool;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * ToolMapper 工具 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface ToolMapper extends BatchBaseMapper<Tool> {
}
