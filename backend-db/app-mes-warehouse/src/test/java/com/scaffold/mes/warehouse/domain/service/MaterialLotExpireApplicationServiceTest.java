package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;

/**
 * 到期日早于今天的可用批改为 EXPIRED。
 */
class MaterialLotExpireApplicationServiceTest {

    @Test
    void dueLotsBecomeExpired() {
        ReceiptStore store = mock(ReceiptStore.class);
        MaterialLot first = lot(1L);
        MaterialLot second = lot(2L);
        LocalDate today = LocalDate.of(2026, 10, 16);
        when(store.listDue(today)).thenReturn(List.of(first, second));
        MaterialLotExpireApplicationService service = new MaterialLotExpireApplicationService(store);

        assertThat(service.expireDue(today)).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo("EXPIRED");
        assertThat(second.getStatus()).isEqualTo("EXPIRED");
        verify(store).updateLot(first);
        verify(store).updateLot(second);
    }

    private static MaterialLot lot(Long id) {
        MaterialLot lot = new MaterialLot();
        lot.setId(id);
        lot.setStatus("AVAILABLE");
        return lot;
    }
}
