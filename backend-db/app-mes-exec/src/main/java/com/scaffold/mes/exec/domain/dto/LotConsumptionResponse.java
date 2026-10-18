package com.scaffold.mes.exec.domain.dto;

import java.math.BigDecimal;

/**
 * LotConsumptionResponse 一条物料消耗
 *
 * @param id 消耗
 * @param materialId 物料
 * @param materialLotId 物料批。0 表示没有具体批
 * @param qty 数量。回冲为负数
 * @param source 来源，如 BACKFLUSH、UNDO、SETTLE
 * @param shortage 欠账、未扣到库存时为 true
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record LotConsumptionResponse(Long id, Long materialId, Long materialLotId, BigDecimal qty, String source,
        boolean shortage) {
}
