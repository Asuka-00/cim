package com.scaffold.share.integration.mes.warehouse.provider;

import java.math.BigDecimal;

import com.scaffold.share.integration.mes.warehouse.response.StockDeduction;

/**
 * LinesideStockPort 线边扣料
 * <p>
 * 执行模块不能直接改库存。从最早的一笔够扣的库存里减。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface LinesideStockPort {

    /**
     * 扣减一种物料。
     *
     * @param materialId 物料
     * @param qty 数量
     * @return 被扣的物料批和库位。数量不是正数时为空
     */
    StockDeduction deduct(Long materialId, BigDecimal qty);

    /**
     * 把数量加回原来的物料批和库位。
     *
     * @param materialId 物料
     * @param materialLotId 物料批
     * @param locationId 库位
     * @param qty 数量
     */
    void restore(Long materialId, Long materialLotId, Long locationId, BigDecimal qty);

    /**
     * 线边可用量合计。
     *
     * @param materialId 物料
     * @return 在库减去冻结。没有时为 0
     */
    BigDecimal available(Long materialId);
}
