package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.RouteStepTool;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * RouteStepToolMapper Route 步骤工具要求 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface RouteStepToolMapper extends BatchBaseMapper<RouteStepTool> {
}
