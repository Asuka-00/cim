package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.mes.exec.domain.entity.MaterialConsumption;
import com.scaffold.mes.exec.domain.repository.MaterialConsumptionRepository;
import com.scaffold.share.integration.mes.warehouse.provider.EquipmentBindingPort;
import com.scaffold.share.integration.mes.warehouse.provider.LinesideStockPort;
import com.scaffold.share.integration.mes.warehouse.response.StockDeduction;

/**
 * 出站扣料后追加 BACKFLUSH。撤销时加回库存并写负数 UNDO。已经冲过的不再加回。
 */
class MaterialConsumptionApplicationServiceTest {

    private MaterialConsumptionRepository repository;
    private LinesideStockPort stock;
    private EquipmentBindingPort bindings;
    private MaterialConsumptionApplicationService service;
    private Lot lot;

    @BeforeEach
    void setUp() {
        repository = mock(MaterialConsumptionRepository.class);
        stock = mock(LinesideStockPort.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        bindings = mock(EquipmentBindingPort.class);
        service = new MaterialConsumptionApplicationService(repository, stock, bindings, clock);
        lot = new Lot();
        lot.setId(8L);
        lot.setStepId(7L);
    }

    @Test
    void recordsBackflush() {
        service.record(lot, 15L, new BigDecimal("1.5"), "PER_LOT", new StockDeduction(20L, 3L, 8L), false);

        verify(repository).save(org.mockito.ArgumentMatchers.<MaterialConsumption>argThat(row ->
                row.getLotId().equals(8L) && "BACKFLUSH".equals(row.getSource()) && "PER_LOT".equals(row.getConsumeMode())
                        && row.getQty().compareTo(new BigDecimal("1.5")) == 0 && row.getLocationId().equals(3L)
                        && row.getIsShortage().equals(0)));
    }

    @Test
    void reversePutsQtyBack() {
        when(repository.listByLot(8L)).thenReturn(List.of(backflush()));

        service.reverse(lot, 7L);

        verify(stock).restore(15L, 20L, 3L, new BigDecimal("1.5"));
        verify(repository).save(org.mockito.ArgumentMatchers.<MaterialConsumption>argThat(row ->
                "UNDO".equals(row.getSource()) && row.getQty().compareTo(new BigDecimal("-1.5")) == 0));
    }

    @Test
    void alreadyReversedIsSkipped() {
        MaterialConsumption undo = backflush();
        undo.setSource("UNDO");
        undo.setQty(new BigDecimal("-1.5"));
        when(repository.listByLot(8L)).thenReturn(List.of(backflush(), undo));

        service.reverse(lot, 7L);

        verify(stock, never()).restore(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void scrappedBindingRejectsAdjust() {
        MaterialConsumption original = backflush();
        original.setId(4L);
        original.setBindingId(9L);
        when(repository.findById(4L)).thenReturn(Optional.of(original));
        when(bindings.scrapped(9L)).thenReturn(true);

        assertThatThrownBy(() -> service.adjust(4L, new BigDecimal("1.0")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.CONSUMPTION_ADJUST);
        verify(stock, never()).restore(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void adjustAppendsDeltaAndKeepsOriginal() {
        MaterialConsumption original = backflush();
        original.setId(4L);
        when(repository.findById(4L)).thenReturn(Optional.of(original));

        service.adjust(4L, new BigDecimal("1.0"));

        assertThat(original.getQty()).isEqualByComparingTo("1.5");
        verify(stock).restore(15L, 20L, 3L, new BigDecimal("0.5"));
        verify(repository).save(org.mockito.ArgumentMatchers.<MaterialConsumption>argThat(row ->
                "ADJUST".equals(row.getSource()) && row.getQty().compareTo(new BigDecimal("-0.5")) == 0
                        && row.getAdjustRefId().equals(4L)));
    }

    @Test
    void settleOffsetsDebtAndRecordsRealUse() {
        MaterialConsumption debt = backflush();
        debt.setId(9L);
        debt.setIsShortage(1);
        debt.setMaterialLotId(0L);
        debt.setLocationId(0L);
        when(repository.findById(9L)).thenReturn(Optional.of(debt));
        when(stock.deduct(15L, new BigDecimal("1.5"))).thenReturn(new StockDeduction(20L, 3L, 8L));

        service.settle(9L);

        verify(repository).save(org.mockito.ArgumentMatchers.<MaterialConsumption>argThat(row ->
                "SETTLE".equals(row.getSource()) && row.getQty().compareTo(new BigDecimal("-1.5")) == 0
                        && row.getAdjustRefId().equals(9L)));
        verify(repository).save(org.mockito.ArgumentMatchers.<MaterialConsumption>argThat(row ->
                "BACKFLUSH".equals(row.getSource()) && row.getIsShortage().equals(0) && row.getLocationId().equals(3L)
                        && row.getQty().compareTo(new BigDecimal("1.5")) == 0));
    }

    @Test
    void secondSettleDoesNotDeductAgain() {
        MaterialConsumption debt = backflush();
        debt.setId(9L);
        debt.setIsShortage(1);
        when(repository.findById(9L)).thenReturn(Optional.of(debt));
        when(repository.existsSettle(9L)).thenReturn(true);

        assertThatThrownBy(() -> service.settle(9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.SHORTAGE_UNSETTLED);
        verify(stock, never()).deduct(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void settleStaysOpenWhenStillShort() {
        MaterialConsumption debt = backflush();
        debt.setId(9L);
        debt.setIsShortage(1);
        when(repository.findById(9L)).thenReturn(Optional.of(debt));
        when(stock.deduct(15L, new BigDecimal("1.5"))).thenReturn(null);

        assertThatThrownBy(() -> service.settle(9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.SHORTAGE_UNSETTLED);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private static MaterialConsumption backflush() {
        MaterialConsumption row = new MaterialConsumption();
        row.setLotId(8L);
        row.setStepId(7L);
        row.setMaterialId(15L);
        row.setMaterialLotId(20L);
        row.setLocationId(3L);
        row.setQty(new BigDecimal("1.5"));
        row.setUomId(8L);
        row.setConsumeMode("PER_LOT");
        row.setSource("BACKFLUSH");
        return row;
    }
}
