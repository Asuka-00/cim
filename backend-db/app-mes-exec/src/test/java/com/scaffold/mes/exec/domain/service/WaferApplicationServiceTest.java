package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.WaferBindRequest;
import com.scaffold.mes.exec.domain.dto.WaferBindRequest.WaferPiece;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 绑定片数必须等于 Lot 数量。内部编号唯一，客户编号可以相同。
 */
class WaferApplicationServiceTest {

    private WaferRepository waferRepository;
    private LotRepository lotRepository;
    private WaferApplicationService service;

    @BeforeEach
    void setUp() {
        waferRepository = mock(WaferRepository.class);
        lotRepository = mock(LotRepository.class);
        WaferHistoryApplicationService history = mock(WaferHistoryApplicationService.class);
        GenealogyLinkApplicationService genealogy = mock(GenealogyLinkApplicationService.class);
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot(2L)));
        service = new WaferApplicationService(waferRepository, lotRepository, history, genealogy);
    }

    @Test
    void bindKeepsSameVendorIdAndUpdatesAliveCount() {
        WaferBindRequest request = new WaferBindRequest();
        request.setWafers(List.of(piece("W1", "CUST-1"), piece("W2", "CUST-1")));

        service.bind(8L, request);

        verify(lotRepository).lock(8L);
        ArgumentCaptor<Wafer> saved = ArgumentCaptor.forClass(Wafer.class);
        verify(waferRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Wafer::getWaferCode).containsExactly("W1", "W2");
        assertThat(saved.getAllValues()).extracting(Wafer::getVendorWaferId).containsExactly("CUST-1", "CUST-1");
        assertThat(saved.getAllValues()).extracting(Wafer::getStatus).containsOnly("NORMAL");
        assertThat(saved.getAllValues()).extracting(Wafer::getSlotNo).containsExactly(1, 2);
        ArgumentCaptor<Lot> updated = ArgumentCaptor.forClass(Lot.class);
        verify(lotRepository).update(updated.capture());
        assertThat(updated.getValue().getWaferCount()).isEqualTo(2);
    }

    @Test
    void bindGeneratesCodesFromLotNo() {
        WaferBindRequest request = new WaferBindRequest();
        request.setCount(2);

        service.bind(8L, request);

        ArgumentCaptor<Wafer> saved = ArgumentCaptor.forClass(Wafer.class);
        verify(waferRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Wafer::getWaferCode).containsExactly("L-1.1", "L-1.2");
    }

    @Test
    void bindRejectsWhenCountDiffersFromLotQty() {
        WaferBindRequest request = new WaferBindRequest();
        request.setWafers(List.of(piece("W1", null)));

        assertThatThrownBy(() -> service.bind(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WAFER_QTY_MISMATCH);
        verify(waferRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bindRejectsExistingCode() {
        when(waferRepository.findByCode("W1")).thenReturn(Optional.of(new Wafer()));
        WaferBindRequest request = new WaferBindRequest();
        request.setWafers(List.of(piece("W1", null), piece("W2", null)));

        assertThatThrownBy(() -> service.bind(8L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WAFER_CODE_EXISTS);
    }

    private static Lot lot(long qty) {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setLotNo("L-1");
        lot.setQty(qty);
        lot.setWaferCount(0);
        return lot;
    }

    private static WaferPiece piece(String code, String vendor) {
        WaferPiece piece = new WaferPiece();
        piece.setWaferCode(code);
        piece.setVendorWaferId(vendor);
        return piece;
    }
}
