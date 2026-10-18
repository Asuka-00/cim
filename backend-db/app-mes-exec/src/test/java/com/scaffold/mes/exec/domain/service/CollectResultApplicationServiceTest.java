package com.scaffold.mes.exec.domain.service;

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

import com.scaffold.mes.core.domain.dto.txn.MesTxnCommand;
import com.scaffold.mes.core.domain.service.txn.MesLockTarget;
import com.scaffold.mes.core.domain.service.txn.MesTransactionService;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.CollectResult;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.CollectResultRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.CollectSpecPort;
import com.scaffold.share.integration.mes.base.response.CollectLimit;

/**
 * 加工中的 Lot 可以保存原始采集值。判定固定为 NA。
 */
class CollectResultApplicationServiceTest {

    private LotRepository lots;
    private CollectResultRepository results;
    private CollectSpecPort specs;
    private LotHoldApplicationService holds;
    private MesTransactionService transactions;
    private CollectResultApplicationService service;

    @BeforeEach
    void setUp() {
        lots = mock(LotRepository.class);
        results = mock(CollectResultRepository.class);
        specs = mock(CollectSpecPort.class);
        holds = mock(LotHoldApplicationService.class);
        transactions = mock(MesTransactionService.class);
        when(transactions.execute(any(), any(), eq(Boolean.class))).thenAnswer(invocation -> {
            Supplier<Boolean> work = invocation.getArgument(1);
            return work.get();
        });
        when(lots.findById(8L)).thenReturn(Optional.of(running()));
        when(specs.limit(21L, 10, 7L, 15L, 2L)).thenReturn(Optional.empty());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new CollectResultApplicationService(lots, results, specs, holds, transactions, clock);
    }

    @Test
    void runningLotSavesRawValue() {
        service.record(8L, 15L, new BigDecimal("1.25"));

        verify(results).save(org.mockito.ArgumentMatchers.<CollectResult>argThat(row ->
                row.getValueNum().compareTo(new BigDecimal("1.25")) == 0
                        && "ORIGINAL".equals(row.getRecordType())
                        && "NA".equals(row.getJudgeResult())
                        && row.getStepId().equals(7L)
                        && row.getItemId().equals(15L)));
        org.mockito.ArgumentCaptor<MesTxnCommand> command = org.mockito.ArgumentCaptor.forClass(MesTxnCommand.class);
        verify(transactions).execute(command.capture(), any(), eq(Boolean.class));
        assertThat(command.getValue().getTxnType()).isEqualTo("COLLECT");
        assertThat(command.getValue().getLocks()).extracting(MesLockTarget::kind).containsExactly("LOT");
    }

    @Test
    void waitingLotRejected() {
        Lot lot = running();
        lot.setStatus("WAITING");
        when(lots.findById(8L)).thenReturn(Optional.of(lot));

        assertThatThrownBy(() -> service.record(8L, 15L, new BigDecimal("1.25")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.COLLECT_NOT_RUNNING);
    }

    @Test
    void belowLimitIsLow() {
        when(specs.limit(21L, 10, 7L, 15L, 2L)).thenReturn(Optional.of(new CollectLimit(new BigDecimal("1"), new BigDecimal("2"))));

        service.record(8L, 15L, new BigDecimal("0.5"));

        verify(results).save(org.mockito.ArgumentMatchers.<CollectResult>argThat(row -> "OOS_LOW".equals(row.getJudgeResult())));
    }

    @Test
    void aboveLimitIsHigh() {
        when(specs.limit(21L, 10, 7L, 15L, 2L)).thenReturn(Optional.of(new CollectLimit(new BigDecimal("1"), new BigDecimal("2"))));

        service.record(8L, 15L, new BigDecimal("2.1"));

        verify(results).save(org.mockito.ArgumentMatchers.<CollectResult>argThat(row -> "OOS_HIGH".equals(row.getJudgeResult())));
    }

    @Test
    void insideLimitIsOk() {
        when(specs.limit(21L, 10, 7L, 15L, 2L)).thenReturn(Optional.of(new CollectLimit(new BigDecimal("1"), new BigDecimal("2"))));

        service.record(8L, 15L, new BigDecimal("1"));

        verify(results).save(org.mockito.ArgumentMatchers.<CollectResult>argThat(row -> "OK".equals(row.getJudgeResult())));
        verify(holds, never()).holdSystem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void oosHoldsWhenEnabled() {
        when(specs.limit(21L, 10, 7L, 15L, 2L))
                .thenReturn(Optional.of(new CollectLimit(new BigDecimal("1"), new BigDecimal("2"), true, "OOS")));

        service.record(8L, 15L, new BigDecimal("0.5"));

        verify(holds).holdSystem(8L, "OOS", "OOS", null);
    }

    @Test
    void effectiveValueIsTheLatest() {
        CollectResult latest = new CollectResult();
        latest.setRecordType("ORIGINAL");
        latest.setValueNum(new BigDecimal("3.5"));
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));

        assertThat(service.effective(8L, 15L, 10, 1)).contains(new BigDecimal("3.5"));
    }

    @Test
    void voidLatestHasNoEffectiveValue() {
        CollectResult latest = new CollectResult();
        latest.setRecordType("VOID");
        latest.setValueNum(new BigDecimal("3.5"));
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));

        assertThat(service.effective(8L, 15L, 10, 1)).isEmpty();
    }

    @Test
    void voidAppendsAndClearsEffectiveValue() {
        CollectResult latest = new CollectResult();
        latest.setId(4L);
        latest.setRecordType("ORIGINAL");
        latest.setValueNum(new BigDecimal("3.5"));
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));

        service.voidLatest(8L, 15L);

        verify(results).save(org.mockito.ArgumentMatchers.<CollectResult>argThat(row ->
                "VOID".equals(row.getRecordType()) && row.getRefId().equals(4L) && row.getValueNum() == null));
    }

    @Test
    void voidRejectedWhenAlreadyVoid() {
        CollectResult latest = new CollectResult();
        latest.setRecordType("VOID");
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.voidLatest(8L, 15L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.COLLECT_VOID);
    }

    @Test
    void voidReleasesOosHold() {
        CollectResult latest = new CollectResult();
        latest.setId(4L);
        latest.setRecordType("ORIGINAL");
        latest.setJudgeResult("OOS_LOW");
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));
        when(specs.limit(21L, 10, 7L, 15L, 2L))
                .thenReturn(Optional.of(new CollectLimit(new BigDecimal("1"), new BigDecimal("2"), true, "OOS")));

        service.voidLatest(8L, 15L);

        verify(holds).releaseSystem(8L, "OOS", "OOS");
        org.mockito.ArgumentCaptor<MesTxnCommand> command = org.mockito.ArgumentCaptor.forClass(MesTxnCommand.class);
        verify(transactions).execute(command.capture(), any(), eq(Boolean.class));
        assertThat(command.getValue().getTxnType()).isEqualTo("UNDO_COLLECT");
    }

    @Test
    void correctionBecomesTheEffectiveValue() {
        CollectResult latest = new CollectResult();
        latest.setId(4L);
        latest.setRecordType("ORIGINAL");
        latest.setValueNum(new BigDecimal("3.5"));
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));

        service.correct(8L, 15L, new BigDecimal("1.1"));

        verify(results).save(org.mockito.ArgumentMatchers.<CollectResult>argThat(row ->
                "CORRECTION".equals(row.getRecordType()) && row.getRefId().equals(4L)
                        && row.getValueNum().compareTo(new BigDecimal("1.1")) == 0));
    }

    @Test
    void correctionOutOfSpecHolds() {
        CollectResult latest = new CollectResult();
        latest.setId(4L);
        latest.setRecordType("ORIGINAL");
        latest.setJudgeResult("OK");
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));
        when(specs.limit(21L, 10, 7L, 15L, 2L))
                .thenReturn(Optional.of(new CollectLimit(new BigDecimal("1"), new BigDecimal("2"), true, "OOS")));

        service.correct(8L, 15L, new BigDecimal("0.2"));

        verify(holds).holdSystem(8L, "OOS", "OOS", null);
    }

    @Test
    void correctionBackInSpecReleases() {
        CollectResult latest = new CollectResult();
        latest.setId(4L);
        latest.setRecordType("ORIGINAL");
        latest.setJudgeResult("OOS_HIGH");
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));
        when(specs.limit(21L, 10, 7L, 15L, 2L))
                .thenReturn(Optional.of(new CollectLimit(new BigDecimal("1"), new BigDecimal("2"), true, "OOS")));

        service.correct(8L, 15L, new BigDecimal("1.5"));

        verify(holds).releaseSystem(8L, "OOS", "OOS");
    }

    @Test
    void correctionRejectedWhenVoid() {
        CollectResult latest = new CollectResult();
        latest.setRecordType("VOID");
        when(results.latest(8L, 15L, 10, 1)).thenReturn(Optional.of(latest));

        assertThatThrownBy(() -> service.correct(8L, 15L, new BigDecimal("1.1")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.COLLECT_CORRECT);
    }

    private static Lot running() {
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setStatus("RUNNING");
        lot.setStepId(7L);
        lot.setRouteSeqNo(10);
        lot.setPassNo(1);
        lot.setProductId(2L);
        lot.setRouteVersionId(21L);
        return lot;
    }
}
