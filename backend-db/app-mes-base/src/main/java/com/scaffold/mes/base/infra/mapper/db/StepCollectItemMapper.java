package com.scaffold.mes.base.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.base.domain.entity.StepCollectItem;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * StepCollectItemMapper 站点采集规格 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface StepCollectItemMapper extends BatchBaseMapper<StepCollectItem> {
}
