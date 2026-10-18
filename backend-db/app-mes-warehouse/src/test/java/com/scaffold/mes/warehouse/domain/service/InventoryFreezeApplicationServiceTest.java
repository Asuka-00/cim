package com.scaffold.mes.warehouse.domain.service;

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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.InventoryFreezeRequest;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 库位冻结增加 qty_frozen，不能超过在库数量。
 */
class InventoryFreezeApplicationServiceTest {

    private ReceiptStore store;
    private Inventory inventory;
    private InventoryFreezeApplicationService service;

    @BeforeEach
    void setUp() {
        store = mock(ReceiptStore.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        inventory = new Inventory();
        inventory.setMaterialId(2L);
        inventory.setUomId(1L);
        inventory.setQtyOnHand(new BigDecimal("10"));
        inventory.setQtyFrozen(new BigDecimal("1"));
        when(store.findInventory(5L, 4L)).thenReturn(Optional.of(inventory));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T21:00:00Z"), ZoneOffset.ofHours(8));
        service = new InventoryFreezeApplicationService(store, session, clock);
    }

    @Test
    void freezeIncreasesFrozenQty() {
        service.freeze(request(new BigDecimal("3")));

        assertThat(inventory.getQtyFrozen()).isEqualByComparingTo("4");
        assertThat(inventory.getQtyOnHand()).isEqualByComparingTo("10");
        verify(store).saveTxn(org.mockito.ArgumentMatchers.<InventoryTxn>argThat(row -> "FREEZE".equals(row.getTxnType())));
    }

    @Test
    void freezeAboveOnHandRejected() {
        assertThatThrownBy(() -> service.freeze(request(new BigDecimal("10"))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.INVENTORY_FREEZE_QTY);
        assertThat(inventory.getQtyFrozen()).isEqualByComparingTo("1");
        verify(store, never()).updateInventory(inventory);
    }

    @Test
    void unfreezeReleasesFrozenQty() {
        inventory.setQtyFrozen(new BigDecimal("4"));

        service.unfreeze(request(new BigDecimal("3")));

        assertThat(inventory.getQtyFrozen()).isEqualByComparingTo("1");
        verify(store).saveTxn(org.mockito.ArgumentMatchers.<InventoryTxn>argThat(row -> "UNFREEZE".equals(row.getTxnType())));
    }

    @Test
    void unfreezeAboveFrozenRejected() {
        assertThatThrownBy(() -> service.unfreeze(request(new BigDecimal("2"))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.INVENTORY_UNFREEZE_QTY);
        assertThat(inventory.getQtyFrozen()).isEqualByComparingTo("1");
    }

    private static InventoryFreezeRequest request(BigDecimal qty) {
        InventoryFreezeRequest request = new InventoryFreezeRequest();
        request.setMaterialLotId(5L);
        request.setLocationId(4L);
        request.setQty(qty);
        return request;
    }
}
