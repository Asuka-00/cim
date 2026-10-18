package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.LotHold;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotHoldMapper Lot Hold Mapper
 * <p>
 * 映射 mes_lot_hold。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotHoldMapper extends BatchBaseMapper<LotHold> {
}
