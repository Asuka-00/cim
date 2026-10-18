package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferHistoryRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ConfirmPort;

/**
 * 取消报废恢复片状态，并把数量加回 Lot。整批报废时 Lot 回到报废前状态。
 */
class WaferUnscrapApplicationServiceTest {

    private WaferRepository waferRepository;
    private ConfirmPort confirms;
    private Wafer wafer;
    private Lot lot;
    private WaferUnscrapApplicationService service;

    @BeforeEach
    void setUp() {
        waferRepository = mock(WaferRepository.class);
        WaferHistoryRepository waferHistoryRepository = mock(WaferHistoryRepository.class);
        LotRepository lotRepository = mock(LotRepository.class);
        LotHistoryRepository lotHistoryRepository = mock(LotHistoryRepository.class);
        MesTransactionService transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        wafer = new Wafer();
        wafer.setId(9L);
        wafer.setLotId(8L);
        wafer.setStatus("SCRAPPED");
        wafer.setScrapReason("BROKEN");
        wafer.setSlotNo(1);
        lot = new Lot();
        lot.setId(8L);
        lot.setStatus("SCRAPPED");
        lot.setQty(0L);
        lot.setWaferCount(0);
        lot.setScrapQty(1L);
        lot.setCurrentScrapQty(1L);
        lot.setWoId(4L);
        when(waferRepository.findById(9L)).thenReturn(Optional.of(wafer));
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        WaferHistory scrap = new WaferHistory();
        scrap.setBeforeJson("{\"status\":\"NORMAL\"}");
        when(waferHistoryRepository.findLatest(9L, "SCRAP")).thenReturn(Optional.of(scrap));
        LotHistory lotScrap = new LotHistory();
        lotScrap.setBeforeJson("{\"status\":\"WAITING\",\"qty\":1}");
        when(lotHistoryRepository.findLatest(8L, "SCRAP")).thenReturn(Optional.of(lotScrap));
        confirms = mock(ConfirmPort.class);
        when(confirms.allowed("UNSCRAP", "LOT", 8L)).thenReturn(true);
        service = new WaferUnscrapApplicationService(transactions, waferRepository, waferHistoryRepository, lotRepository,
                lotHistoryRepository, mock(WaferHistoryApplicationService.class), mock(LotHistoryApplicationService.class),
                confirms);
    }

    @Test
    void unscrapRestoresWaferAndLot() {
        service.unscrap(9L);

        assertThat(wafer.getStatus()).isEqualTo("NORMAL");
        assertThat(wafer.getScrapReason()).isNull();
        assertThat(lot.getStatus()).isEqualTo("WAITING");
        assertThat(lot.getQty()).isEqualTo(1L);
        assertThat(lot.getScrapQty()).isZero();
    }

    @Test
    void missingConfirmBlocksUnscrap() {
        when(confirms.allowed("UNSCRAP", "LOT", 8L)).thenReturn(false);

        assertThatThrownBy(() -> service.unscrap(9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.CONFIRM_REQUIRED);
        verify(waferRepository, never()).update(any());
    }

    @Test
    void normalWaferRejected() {
        wafer.setStatus("NORMAL");

        assertThatThrownBy(() -> service.unscrap(9L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WAFER_UNSCRAP_STATUS);
    }
}
