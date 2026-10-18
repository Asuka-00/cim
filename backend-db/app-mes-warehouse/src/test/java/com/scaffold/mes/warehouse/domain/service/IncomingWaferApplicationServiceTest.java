package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.IncomingWaferLine;
import com.scaffold.mes.warehouse.domain.entity.IncomingWafer;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.exception.BusinessException;

/**
 * 来料 Wafer 挂到物料批上，状态为 AVAILABLE。重复编号拒绝。
 */
class IncomingWaferApplicationServiceTest {

    private ReceiptStore store;
    private IncomingWaferApplicationService service;

    @BeforeEach
    void setUp() {
        store = mock(ReceiptStore.class);
        when(store.findLotById(5L)).thenReturn(Optional.of(new MaterialLot()));
        when(store.incomingExists(5L, "VW-1")).thenReturn(false);
        when(store.incomingExists(5L, "VW-2")).thenReturn(false);
        service = new IncomingWaferApplicationService(store);
    }

    @Test
    void recordSavesAvailable() {
        service.record(5L, List.of(line("VW-1", 1), line("VW-2", 2)));

        verify(store).saveIncoming(org.mockito.ArgumentMatchers.<IncomingWafer>argThat(row ->
                "VW-1".equals(row.getVendorWaferId()) && "AVAILABLE".equals(row.getStatus())
                        && row.getSlotNo().equals(1) && row.getMaterialLotId().equals(5L)));
    }

    @Test
    void duplicateRejected() {
        when(store.incomingExists(5L, "VW-1")).thenReturn(true);

        assertThatThrownBy(() -> service.record(5L, List.of(line("VW-1", 1))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.INCOMING_WAFER_EXISTS);
        verify(store, never()).saveIncoming(org.mockito.ArgumentMatchers.any());
    }

    private static IncomingWaferLine line(String code, int slot) {
        IncomingWaferLine line = new IncomingWaferLine();
        line.setVendorWaferId(code);
        line.setSlotNo(slot);
        return line;
    }
}
