package com.scaffold.mes.warehouse.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.warehouse.domain.MesWarehouseErrorCodes;
import com.scaffold.mes.warehouse.domain.dto.InspectionRequest;
import com.scaffold.mes.warehouse.domain.entity.IncomingInspection;
import com.scaffold.mes.warehouse.domain.entity.MaterialLot;
import com.scaffold.mes.warehouse.domain.repository.ReceiptStore;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 合格变为可用，不合格进隔离。特采没有说明时拒绝。
 */
class IncomingInspectionApplicationServiceTest {

    private ReceiptStore store;
    private MaterialLot lot;
    private IncomingInspectionApplicationService service;

    @BeforeEach
    void setUp() {
        store = mock(ReceiptStore.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        lot = new MaterialLot();
        lot.setId(5L);
        lot.setStatus("PENDING_QC");
        when(store.findLotById(5L)).thenReturn(Optional.of(lot));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T19:00:00Z"), ZoneOffset.ofHours(8));
        service = new IncomingInspectionApplicationService(store, session, clock);
    }

    @Test
    void passBecomesAvailable() {
        service.inspect(5L, request("PASS", null));

        assertThat(lot.getStatus()).isEqualTo("AVAILABLE");
        assertThat(lot.getQcResult()).isEqualTo("PASS");
        verify(store).saveInspection(org.mockito.ArgumentMatchers.<IncomingInspection>argThat(row ->
                "PASS".equals(row.getResult()) && "sam".equals(row.getInspector())));
    }

    @Test
    void failGoesToQuarantine() {
        service.inspect(5L, request("FAIL", "外观"));

        assertThat(lot.getStatus()).isEqualTo("QUARANTINE");
        assertThat(lot.getQcResult()).isEqualTo("FAIL");
    }

    @Test
    void concessionNeedsRemark() {
        assertThatThrownBy(() -> service.inspect(5L, request("CONCESSION", " ")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesWarehouseErrorCodes.INSPECTION_REMARK);
        verify(store, never()).updateLot(lot);
    }

    private static InspectionRequest request(String result, String remark) {
        InspectionRequest request = new InspectionRequest();
        request.setResult(result);
        request.setRemark(remark);
        return request;
    }
}
