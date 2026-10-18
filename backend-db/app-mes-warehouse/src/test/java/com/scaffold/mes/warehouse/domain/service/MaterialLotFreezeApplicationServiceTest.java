package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.exception.BusinessException;

/**
 * 可用物料批可以整批冻结。待检批不能冻。
 */
class MaterialLotFreezeApplicationServiceTest {

    private ReceiptStore store;
    private MaterialLot lot;
    private MaterialLotFreezeApplicationService service;

    @BeforeEach
    void setUp() {
        store = mock(ReceiptStore.class);
        lot = new MaterialLot();
        lot.setId(5L);
        lot.setStatus("AVAILABLE");
        when(store.findLotById(5L)).thenReturn(Optional.of(lot));
        service = new MaterialLotFreezeApplicationService(store);
    }

    @Test
    void freezeMarksFrozen() {
        service.freeze(5L);

        assertThat(lot.getStatus()).isEqualTo("FROZEN");
        verify(store).updateLot(lot);
    }

    @Test
    void pendingCannotFreeze() {
        lot.setStatus("PENDING_QC");

        assertThatThrownBy(() -> service.freeze(5L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.FREEZE_STATUS);
        verify(store, never()).updateLot(lot);
    }

    @Test
    void unfreezeRestoresAvailable() {
        lot.setStatus("FROZEN");

        service.unfreeze(5L);

        assertThat(lot.getStatus()).isEqualTo("AVAILABLE");
        verify(store).updateLot(lot);
    }

    @Test
    void availableCannotUnfreeze() {
        assertThatThrownBy(() -> service.unfreeze(5L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.UNFREEZE_STATUS);
        verify(store, never()).updateLot(lot);
    }
}
