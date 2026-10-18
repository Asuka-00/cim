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
import com.scaffold.mes.warehouse.domain.dto.IssueRequest;
import com.scaffold.mes.warehouse.domain.entity.Inventory;
import com.scaffold.mes.warehouse.domain.entity.InventoryTxn;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 领料只接受可用批，并按可用数量扣源库位。
 */
class IssueApplicationServiceTest {

    private ReceiptStore store;
    private Inventory source;
    private MaterialLot lot;
    private IssueApplicationService service;

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
        lot = new MaterialLot();
        lot.setId(5L);
        lot.setMaterialId(2L);
        lot.setUomId(1L);
        lot.setStatus("AVAILABLE");
        when(store.findLotById(5L)).thenReturn(Optional.of(lot));
        source = new Inventory();
        source.setQtyOnHand(new BigDecimal("10"));
        source.setQtyFrozen(new BigDecimal("1"));
        when(store.findInventory(5L, 4L)).thenReturn(Optional.of(source));
        when(store.findInventory(5L, 8L)).thenReturn(Optional.empty());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T20:00:00Z"), ZoneOffset.ofHours(8));
        service = new IssueApplicationService(transactions, store, session, clock);
    }

    @Test
    void issueMovesAvailableQty() {
        service.issue(request(new BigDecimal("4")));

        assertThat(source.getQtyOnHand()).isEqualByComparingTo("6");
        verify(store).saveInventory(org.mockito.ArgumentMatchers.<Inventory>argThat(row ->
                row.getLocationId().equals(8L) && row.getQtyOnHand().compareTo(new BigDecimal("4")) == 0));
        verify(store).saveTxn(org.mockito.ArgumentMatchers.<InventoryTxn>argThat(row -> "ISSUE".equals(row.getTxnType())));
    }

    @Test
    void returnMovesQtyBack() {
        service.returnMaterial(request(new BigDecimal("4")));

        assertThat(source.getQtyOnHand()).isEqualByComparingTo("6");
        verify(store).saveTxn(org.mockito.ArgumentMatchers.<InventoryTxn>argThat(row ->
                "RETURN".equals(row.getTxnType()) && row.getToLocationId().equals(8L)));
    }

    @Test
    void frozenLotRejected() {
        lot.setStatus("FROZEN");

        assertThatThrownBy(() -> service.issue(request(new BigDecimal("1"))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.ISSUE_STATUS);
        verify(store, never()).updateInventory(any());
    }

    @Test
    void expiredLotRejected() {
        lot.setStatus("EXPIRED");

        assertThatThrownBy(() -> service.issue(request(new BigDecimal("1"))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.ISSUE_STATUS);
        verify(store, never()).updateInventory(any());
    }

    @Test
    void pendingLotRejected() {
        lot.setStatus("PENDING_QC");

        assertThatThrownBy(() -> service.issue(request(new BigDecimal("1"))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.ISSUE_STATUS);
        verify(store, never()).updateInventory(any());
    }

    @Test
    void frozenQtyBlocksIssue() {
        assertThatThrownBy(() -> service.issue(request(new BigDecimal("10"))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.ISSUE_QTY);
        assertThat(source.getQtyOnHand()).isEqualByComparingTo("10");
    }

    private static IssueRequest request(BigDecimal qty) {
        IssueRequest request = new IssueRequest();
        request.setMaterialLotId(5L);
        request.setFromLocationId(4L);
        request.setToLocationId(8L);
        request.setQty(qty);
        return request;
    }
}
