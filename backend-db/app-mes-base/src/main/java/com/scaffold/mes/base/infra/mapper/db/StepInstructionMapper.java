package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.StepInstruction;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StepInstructionMapper 站点作业指导 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StepInstructionMapper extends BatchBaseMapper<StepInstruction> {
}
