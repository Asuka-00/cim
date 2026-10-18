package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.entity.WoMaterialRequirement;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.WoMaterialRequirementRepository;
import com.scaffold.share.integration.mes.base.provider.BomRequirementPort;
import com.scaffold.share.integration.mes.base.provider.StepAppearancePort;
import com.scaffold.share.integration.mes.warehouse.provider.LinesideStockPort;
import com.scaffold.share.integration.mes.base.response.BomRequirementLine;

/**
 * 按片、die、条、颗按计划数量和损耗率算出需求。按批只登记，需求数量为空。
 */
class WoMaterialRequirementApplicationServiceTest {

    @Test
    void expandsCountableQtyAndLeavesPerLotEmpty() {
        BomRequirementPort bom = mock(BomRequirementPort.class);
        WoMaterialRequirementRepository repository = mock(WoMaterialRequirementRepository.class);
        when(bom.list(31L)).thenReturn(List.of(
                new BomRequirementLine(7L, 15L, "PER_DIE", new BigDecimal("2"), new BigDecimal("5"), 3L, 0),
                new BomRequirementLine(8L, 16L, "PER_LOT", new BigDecimal("1"), null, 3L, 0)));
        StepAppearancePort appearances = mock(StepAppearancePort.class);
        when(appearances.count(21L, 3L)).thenReturn(2);
        LinesideStockPort stock = mock(LinesideStockPort.class);
        when(stock.available(15L)).thenReturn(new BigDecimal("100"));
        when(stock.available(16L)).thenReturn(BigDecimal.ZERO);
        WoMaterialRequirementApplicationService service = new WoMaterialRequirementApplicationService(bom, appearances,
                stock, repository);
        WorkOrder order = new WorkOrder();
        order.setId(4L);
        order.setBomVersionId(31L);
        order.setRouteVersionId(21L);
        order.setPlanQty(10L);

        assertThat(service.expand(order)).isEmpty();

        verify(repository).save(org.mockito.ArgumentMatchers.<WoMaterialRequirement>argThat(row ->
                row.getBomItemId().equals(7L) && row.getRequiredQty().compareTo(new BigDecimal("42.0")) == 0));
        verify(repository).save(org.mockito.ArgumentMatchers.<WoMaterialRequirement>argThat(row ->
                row.getBomItemId().equals(8L) && row.getRequiredQty() == null));
    }

    @Test
    void shortLinesideWarnsWithoutBlocking() {
        BomRequirementPort bom = mock(BomRequirementPort.class);
        WoMaterialRequirementRepository repository = mock(WoMaterialRequirementRepository.class);
        when(bom.list(31L)).thenReturn(List.of(
                new BomRequirementLine(7L, 15L, "PER_DIE", new BigDecimal("2"), null, 3L, 10)));
        StepAppearancePort appearances = mock(StepAppearancePort.class);
        LinesideStockPort stock = mock(LinesideStockPort.class);
        when(stock.available(15L)).thenReturn(new BigDecimal("10"));
        WoMaterialRequirementApplicationService service = new WoMaterialRequirementApplicationService(bom, appearances,
                stock, repository);
        WorkOrder order = new WorkOrder();
        order.setId(4L);
        order.setBomVersionId(31L);
        order.setPlanQty(10L);

        assertThat(service.expand(order)).containsExactly("物料 15");
        verify(repository).save(org.mockito.ArgumentMatchers.any());
    }
}
