package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.repository.WaferHistoryRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * Lot 级 Track 不写 Wafer 历史。片级报废保留快照，拆批丢掉快照。
 */
class WaferHistoryApplicationServiceTest {

    private WaferHistoryRepository repository;
    private WaferHistoryApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(WaferHistoryRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T06:00:00Z"), ZoneOffset.ofHours(8));
        service = new WaferHistoryApplicationService(repository, session, clock);
    }

    @Test
    void lotTrackIsRejected() {
        assertThatThrownBy(() -> service.record(wafer(), lot(), "TRACK_OUT", false, 1, "NORMAL", null, null, "{}"))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.WAFER_HISTORY_LOT_TRACK);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void scrapKeepsSnapshot() {
        service.record(wafer(), lot(), "SCRAP", false, 1, "NORMAL", "S1", null, "{\"status\":\"NORMAL\"}");

        verify(repository).save(org.mockito.ArgumentMatchers.<WaferHistory>argThat(row ->
                "SCRAP".equals(row.getTxnType()) && "{\"status\":\"NORMAL\"}".equals(row.getBeforeJson())));
    }

    @Test
    void splitDropsSnapshot() {
        service.record(wafer(), lot(), "SPLIT", false, 1, "NORMAL", null, null, "{\"lotId\":8}");

        verify(repository).save(org.mockito.ArgumentMatchers.<WaferHistory>argThat(row -> row.getBeforeJson() == null));
    }

    private static Wafer wafer() {
        Wafer wafer = new Wafer();
        wafer.setId(9L);
        wafer.setWaferCode("W1");
        wafer.setSlotNo(2);
        wafer.setStatus("SCRAPPED");
        return wafer;
    }

    private static Lot lot() {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setLotNo("L-1");
        lot.setRouteSeqNo(10);
        lot.setStepId(7L);
        lot.setPassNo(1);
        return lot;
    }
}
