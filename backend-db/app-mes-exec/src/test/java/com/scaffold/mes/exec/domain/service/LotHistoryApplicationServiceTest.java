package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 只有可撤销事务保留变更前快照。Hold 即使传入快照也会被丢掉。
 */
class LotHistoryApplicationServiceTest {

    private LotHistoryRepository repository;
    private LotHistoryApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LotHistoryRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        org.mockito.Mockito.when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T06:00:00Z"), ZoneOffset.ofHours(8));
        service = new LotHistoryApplicationService(repository, session, clock);
    }

    @Test
    void trackOutKeepsSnapshot() {
        service.record(lot(), "TRACK_OUT", null, null, "{\"status\":\"RUNNING\"}", "RUNNING", 2L);

        verify(repository).save(org.mockito.ArgumentMatchers.<LotHistory>argThat(row ->
                "TRACK_OUT".equals(row.getTxnType())
                        && "{\"status\":\"RUNNING\"}".equals(row.getBeforeJson())
                        && "WAITING".equals(row.getStatusAfter())
                        && row.getQtyAfter().equals(2L)
                        && Long.valueOf(4L).equals(row.getBatchId())));
    }

    @Test
    void holdDropsSnapshot() {
        service.record(lot(), "HOLD", "Q1", "wait", "{\"status\":\"WAITING\"}", "WAITING", 2L);

        verify(repository).save(org.mockito.ArgumentMatchers.<LotHistory>argThat(row ->
                row.getBeforeJson() == null && "Q1".equals(row.getReasonCode()) && "sam".equals(row.getOperator())));
    }

    @Test
    void blankTypeRejected() {
        assertThatThrownBy(() -> service.record(lot(), " ", null, null, null, "WAITING", 2L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.LOT_HISTORY_TYPE_REQUIRED);
    }

    private static Lot lot() {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setLotNo("L-1");
        lot.setStatus("WAITING");
        lot.setQty(2L);
        lot.setRouteSeqNo(10);
        lot.setStepId(7L);
        lot.setPassNo(1);
        lot.setBatchId(4L);
        return lot;
    }
}
