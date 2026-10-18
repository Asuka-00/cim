package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.StepTool;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StepToolMapper 站点工具要求 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StepToolMapper extends BatchBaseMapper<StepTool> {
}
