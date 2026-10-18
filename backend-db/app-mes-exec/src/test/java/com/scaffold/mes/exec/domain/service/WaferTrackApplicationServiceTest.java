package com.scaffold.mes.exec.domain.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.dto.WaferTrackRequest;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;

/**
 * 片级进站时，没选中的片留在原步骤并拆成子 Lot。选中的片写片级历史。
 */
class WaferTrackApplicationServiceTest {

    private LotSplitApplicationService split;
    private TrackInApplicationService trackIn;
    private WaferHistoryApplicationService waferHistory;
    private Wafer first;
    private WaferTrackApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lotRepository = mock(LotRepository.class);
        WaferRepository waferRepository = mock(WaferRepository.class);
        split = mock(LotSplitApplicationService.class);
        trackIn = mock(TrackInApplicationService.class);
        waferHistory = mock(WaferHistoryApplicationService.class);
        first = wafer(1L);
        Wafer second = wafer(2L);
        when(waferRepository.listByLot(8L)).thenReturn(List.of(first, second));
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setStatus("RUNNING");
        when(lotRepository.findById(8L)).thenReturn(Optional.of(lot));
        service = new WaferTrackApplicationService(lotRepository, waferRepository, split, trackIn,
                mock(TrackOutApplicationService.class), waferHistory);
    }

    @Test
    void partialTrackInSplitsTheRest() {
        WaferTrackRequest request = new WaferTrackRequest();
        request.setWaferIds(List.of(1L));
        request.setStepId(7L);
        request.setEquipmentId(3L);

        service.trackIn(8L, request);

        verify(split).splitLeaving(8L, List.of(2L));
        verify(trackIn).trackIn(org.mockito.ArgumentMatchers.eq(8L), org.mockito.ArgumentMatchers.any());
        verify(waferHistory).record(org.mockito.ArgumentMatchers.eq(first), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq("TRACK_IN"), org.mockito.ArgumentMatchers.eq(true),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull());
    }

    private static Wafer wafer(Long id) {
        Wafer wafer = new Wafer();
        wafer.setId(id);
        wafer.setStatus("NORMAL");
        wafer.setSlotNo(id.intValue());
        return wafer;
    }
}
