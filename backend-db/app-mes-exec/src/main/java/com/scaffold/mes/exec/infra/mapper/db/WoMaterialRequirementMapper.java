package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.WoMaterialRequirement;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * WoMaterialRequirementMapper 工单物料需求 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface WoMaterialRequirementMapper extends BatchBaseMapper<WoMaterialRequirement> {
}
