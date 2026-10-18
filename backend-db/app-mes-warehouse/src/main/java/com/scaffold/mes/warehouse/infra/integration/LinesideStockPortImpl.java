package com.scaffold.mes.warehouse.infra.integration;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.scaffold.mes.warehouse.domain.service.LinesideStockApplicationService;
import com.scaffold.share.integration.mes.warehouse.provider.LinesideStockPort;
import com.scaffold.share.integration.mes.warehouse.response.StockDeduction;

import lombok.RequiredArgsConstructor;

/**
 * LinesideStockPortImpl 线边扣料
 * <p>
 * 转给库存服务扣减。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class LinesideStockPortImpl implements LinesideStockPort {

    private final LinesideStockApplicationService stock;

    @Override
    public StockDeduction deduct(Long materialId, BigDecimal qty) {
        return stock.deduct(materialId, qty);
    }

    @Override
    public void restore(Long materialId, Long materialLotId, Long locationId, BigDecimal qty) {
        stock.restore(materialId, materialLotId, locationId, qty);
    }

    @Override
    public BigDecimal available(Long materialId) {
        return stock.available(materialId);
    }
}
