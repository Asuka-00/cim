package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.WaferReenterRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 报废片可以回收。再入厂生成新编号，客户编号保持，并指回原片。
 */
class WaferReclaimApplicationServiceTest {

    private WaferRepository waferRepository;
    private LotRepository lotRepository;
    private Wafer scrapped;
    private Lot lot;
    private WaferReclaimApplicationService service;

    @BeforeEach
    void setUp() {
        waferRepository = mock(WaferRepository.class);
        lotRepository = mock(LotRepository.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        scrapped = new Wafer();
        scrapped.setId(9L);
        scrapped.setLotId(8L);
        scrapped.setStatus("SCRAPPED");
        scrapped.setVendorWaferId("CUST-1");
        scrapped.setPurpose("PRODUCT");
        scrapped.setSlotNo(1);
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("WAITING");
        lot.setQty(1L);
        lot.setWaferCount(1);
        when(waferRepository.findById(9L)).thenReturn(Optional.of(scrapped));
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        when(waferRepository.listByLot(8L)).thenReturn(List.of(scrapped));
        service = new WaferReclaimApplicationService(transactions, waferRepository, lotRepository,
                mock(WaferHistoryApplicationService.class));
    }

    @Test
    void reclaimScrappedWafer() {
        service.reclaim(9L);

        assertThat(scrapped.getStatus()).isEqualTo("RECLAIMED");
    }

    @Test
    void reenterKeepsVendorAndPointsAtSource() {
        scrapped.setStatus("RECLAIMED");
        WaferReenterRequest request = new WaferReenterRequest();
        request.setReclaimedWaferId(9L);
        request.setWaferCode("W-NEW");

        service.reenter(8L, request);

        verify(waferRepository).save(org.mockito.ArgumentMatchers.<Wafer>argThat(row ->
                "W-NEW".equals(row.getWaferCode())
                        && row.getRecycledFromWaferId().equals(9L)
                        && "CUST-1".equals(row.getVendorWaferId())
                        && "NORMAL".equals(row.getStatus())));
        assertThat(scrapped.getStatus()).isEqualTo("RECLAIMED");
        assertThat(lot.getQty()).isEqualTo(2L);
    }

    @Test
    void normalWaferOnWaitingLotRejected() {
        scrapped.setStatus("NORMAL");
        lot.setStatus("WAITING");

        assertThatThrownBy(() -> service.reclaim(9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WAFER_RECLAIM_REJECTED);
    }
}
