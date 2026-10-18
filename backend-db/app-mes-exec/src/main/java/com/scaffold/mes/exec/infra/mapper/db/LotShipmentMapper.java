package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.LotShipment;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * LotShipmentMapper 出货记录 Mapper
 * <p>
 * 映射 mes_lot_shipment。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface LotShipmentMapper extends BatchBaseMapper<LotShipment> {
}
