package com.scaffold.share.integration.mes.base.response;

import java.math.BigDecimal;

/**
 * BomRequirementLine 工单展开用的一行 BOM
 *
 * @param bomItemId BOM 行
 * @param materialId 物料
 * @param consumeMode 消耗方式
 * @param qtyPer 单位用量
 * @param scrapRate 损耗率，百分比。5 表示 5%
 * @param stepId 消耗站点
 * @param routeSeqNo 步骤序号。0 表示该站点每次经过都消耗
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record BomRequirementLine(Long bomItemId, Long materialId, String consumeMode, BigDecimal qtyPer,
        BigDecimal scrapRate, Long stepId, Integer routeSeqNo) {
}
