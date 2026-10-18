package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.dto.WaferMapImportRequest;
import com.scaffold.mes.exec.domain.entity.LotDieBin;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferMap;
import com.scaffold.mes.exec.domain.repository.DieBinLogRepository;
import com.scaffold.mes.exec.domain.repository.LotDieBinRepository;
import com.scaffold.mes.exec.domain.repository.WaferMapRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.context.CurrentSessionProvider;

/**
 * 导入 Wafer Map 后清掉汇总行，写成逐片 Bin。重测递增版本，并清掉上一版的当前标记。
 */
class WaferMapApplicationServiceTest {

    private WaferMapRepository mapRepository;
    private LotDieBinRepository binRepository;
    private WaferMap previous;
    private WaferMapApplicationService service;

    @BeforeEach
    void setUp() {
        WaferRepository wafers = mock(WaferRepository.class);
        mapRepository = mock(WaferMapRepository.class);
        binRepository = mock(LotDieBinRepository.class);
        DieBinLogRepository logs = mock(DieBinLogRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        Wafer wafer = new Wafer();
        wafer.setId(5L);
        wafer.setLotId(8L);
        when(wafers.findById(5L)).thenReturn(Optional.of(wafer));
        previous = new WaferMap();
        previous.setWaferId(5L);
        previous.setMapVersion(1);
        previous.setIsCurrent(1);
        when(mapRepository.listByWafer(5L)).thenReturn(List.of(previous));
        LotDieBin summary = new LotDieBin();
        summary.setWaferId(0L);
        summary.setBinCode("1");
        summary.setDieQty(10L);
        when(binRepository.listByLot(8L)).thenReturn(List.of(summary));
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T14:00:00Z"), ZoneOffset.ofHours(8));
        service = new WaferMapApplicationService(wafers, mapRepository, binRepository, logs, session, clock);
    }

    @Test
    void importReplacesSummaryWithWaferBins() {
        WaferMapImportRequest request = new WaferMapImportRequest();
        request.setWaferId(5L);
        request.setFileName("a.csv");
        request.setCsv("bin,qty,good\n1,8,1\n2,2,0");

        assertThat(service.importMap(8L, request)).isEqualTo(2);
        assertThat(previous.getIsCurrent()).isZero();
        verify(mapRepository).update(previous);
        verify(binRepository).deleteSummary(8L);
        verify(binRepository).save(org.mockito.ArgumentMatchers.<LotDieBin>argThat(row ->
                row.getWaferId().equals(5L) && "1".equals(row.getBinCode()) && row.getDieQty().equals(8L)
                        && "IMPORT".equals(row.getSource())));
        verify(mapRepository).save(org.mockito.ArgumentMatchers.<WaferMap>argThat(row ->
                row.getMapVersion().equals(2) && row.getIsCurrent().equals(1) && row.getGoodDie().equals(8)
                        && row.getTotalDie().equals(10) && row.getBadDie().equals(2)));
    }
}
