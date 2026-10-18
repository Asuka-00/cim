package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.LotSamplingPlan;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotSamplingPlanMapper Lot 抽检决定 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotSamplingPlanMapper extends BatchBaseMapper<LotSamplingPlan> {
}
