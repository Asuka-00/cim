package com.scaffold.mes.exec.domain.dto;

/**
 * ProductOutputResponse 某一天某个产品的完工量
 *
 * @param productId 产品。空表示 Lot 上没有产品
 * @param lotCount 完成的 Lot 数
 * @param qty 完成数量合计
 * @param scrapQty 报废数量合计
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record ProductOutputResponse(Long productId, long lotCount, long qty, long scrapQty) {
}
