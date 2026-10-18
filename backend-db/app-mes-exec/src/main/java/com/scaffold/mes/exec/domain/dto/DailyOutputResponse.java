package com.scaffold.mes.exec.domain.dto;

/**
 * DailyOutputResponse 某一天的完工量
 *
 * @param lotCount 完成的 Lot 数
 * @param qty 完成数量合计
 * @param scrapQty 报废数量合计
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record DailyOutputResponse(long lotCount, long qty, long scrapQty) {
}
