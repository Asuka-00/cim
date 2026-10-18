package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.LotRelation;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotRelationMapper Lot 关系 Mapper
 * <p>
 * 映射 hist_mes_lot_relation。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotRelationMapper extends BatchBaseMapper<LotRelation> {
}
