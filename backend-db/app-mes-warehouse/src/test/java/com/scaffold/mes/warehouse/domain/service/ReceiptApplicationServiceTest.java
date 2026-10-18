package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.ReceiptRequest;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.entity.StorageLocation;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.mes.warehouse.domain.repository.WarehouseStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 来料收货生成物料批和库存。需要检验时停在待检。
 */
class ReceiptApplicationServiceTest {

    private ReceiptStore store;
    private StorageLocation location;
    private ReceiptApplicationService service;

    @BeforeEach
    void setUp() {
        store = mock(ReceiptStore.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        when(store.findLot("ML-1")).thenReturn(Optional.empty());
        WarehouseStore places = mock(WarehouseStore.class);
        location = new StorageLocation();
        location.setId(4L);
        location.setEnabled(1);
        when(places.findLocationById(4L)).thenReturn(Optional.of(location));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T18:00:00Z"), ZoneOffset.ofHours(8));
        service = new ReceiptApplicationService(transactions, store, places, session, clock);
    }

    @Test
    void receiveWithoutInspectIsAvailable() {
        service.receive(request(false));

        verify(store).saveLot(org.mockito.ArgumentMatchers.<MaterialLot>argThat(row ->
                "AVAILABLE".equals(row.getStatus()) && row.getReceivedQty().compareTo(new BigDecimal("12.5")) == 0));
        verify(store).saveInventory(org.mockito.ArgumentMatchers.<Inventory>argThat(row ->
                row.getQtyOnHand().compareTo(new BigDecimal("12.5")) == 0 && row.getLocationId().equals(4L)));
        verify(store).saveTxn(org.mockito.ArgumentMatchers.<InventoryTxn>argThat(row ->
                "RECEIVE".equals(row.getTxnType()) && "RECEIPT".equals(row.getRefType())));
    }

    @Test
    void inspectStaysPending() {
        service.receive(request(true));

        verify(store).saveLot(org.mockito.ArgumentMatchers.<MaterialLot>argThat(row -> "PENDING_QC".equals(row.getStatus())));
    }

    @Test
    void disabledLocationRejected() {
        location.setEnabled(0);

        assertThatThrownBy(() -> service.receive(request(false)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.LOCATION_DISABLED);
        verify(store, never()).saveLot(any());
    }

    @Test
    void duplicateLotRejected() {
        when(store.findLot("ML-1")).thenReturn(Optional.of(new MaterialLot()));

        assertThatThrownBy(() -> service.receive(request(false)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.RECEIPT_LOT_EXISTS);
        verify(store, never()).saveInventory(any());
    }

    private static ReceiptRequest request(boolean inspect) {
        ReceiptRequest request = new ReceiptRequest();
        request.setReceiptNo("R-1");
        request.setLotCode("ML-1");
        request.setMaterialId(2L);
        request.setLocationId(4L);
        request.setUomId(1L);
        request.setQty(new BigDecimal("12.5"));
        request.setNeedInspect(inspect);
        return request;
    }
}
