package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.DieBinLine;
import com.scaffold.mes.exec.domain.entity.DieBinLog;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotDieBin;
import com.scaffold.mes.exec.domain.repository.DieBinLogRepository;
import com.scaffold.mes.exec.domain.repository.LotDieBinRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 没有逐片明细时可以写 Bin 汇总。良品合计必须等于 Lot 数量。
 */
class DieBinApplicationServiceTest {

    private LotDieBinRepository binRepository;
    private DieBinLogRepository logRepository;
    private DieBinApplicationService service;

    @BeforeEach
    void setUp() {
        LotRepository lots = mock(LotRepository.class);
        binRepository = mock(LotDieBinRepository.class);
        logRepository = mock(DieBinLogRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setQty(10L);
        when(lots.findById(8L)).thenReturn(Optional.of(lot));
        when(binRepository.listByLot(8L)).thenReturn(List.of());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T13:00:00Z"), ZoneOffset.ofHours(8));
        service = new DieBinApplicationService(lots, binRepository, logRepository, session, clock);
    }

    @Test
    void summaryMatchesQty() {
        service.replaceSummary(8L, List.of(line("1", true, 10L), line("2", false, 3L)));

        verify(binRepository).save(org.mockito.ArgumentMatchers.<LotDieBin>argThat(row ->
                row.getWaferId().equals(0L) && "1".equals(row.getBinCode()) && row.getDieQty().equals(10L)
                        && row.getIsGood().equals(1)));
        verify(logRepository).save(org.mockito.ArgumentMatchers.<DieBinLog>argThat(row ->
                "1".equals(row.getBinCode()) && row.getDelta().equals(10L) && "MANUAL".equals(row.getReason())));
    }

    @Test
    void goodSumMustEqualQty() {
        assertThatThrownBy(() -> service.replaceSummary(8L, List.of(line("1", true, 8L))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.DIE_BIN_QTY_MISMATCH);
        verify(binRepository, never()).save(any());
    }

    @Test
    void waferDetailBlocksSummary() {
        LotDieBin detail = new LotDieBin();
        detail.setWaferId(5L);
        when(binRepository.listByLot(8L)).thenReturn(List.of(detail));

        assertThatThrownBy(() -> service.replaceSummary(8L, List.of(line("1", true, 10L))))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.DIE_BIN_DETAIL_EXISTS);
        verify(binRepository, never()).deleteSummary(8L);
    }

    private static DieBinLine line(String code, boolean good, long qty) {
        DieBinLine line = new DieBinLine();
        line.setBinCode(code);
        line.setGood(good);
        line.setDieQty(qty);
        return line;
    }
}
