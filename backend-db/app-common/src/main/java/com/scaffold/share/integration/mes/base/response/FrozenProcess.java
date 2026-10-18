package com.scaffold.share.integration.mes.base.response;

/**
 * FrozenProcess 下工单时固化的工艺
 * <p>
 * 产品版本只绑定 Route 和 BOM 主数据。下工单时取当时的 ACTIVE 版本，写到工单上后不再跟随升版。
 *
 * @param productId 产品
 * @param productVersionId 产品版本
 * @param routeVersionId 当时生效的 Route 版本
 * @param bomVersionId 当时生效的 BOM 版本
 * @param customerId 产品上的客户，可空
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public record FrozenProcess(Long productId, Long productVersionId, Long routeVersionId, Long bomVersionId, Long customerId) {
}
