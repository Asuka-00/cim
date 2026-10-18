package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.repository.txn.MesTxnReversalRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.GenealogyLink;
import com.scaffold.mes.exec.domain.repository.GenealogyLinkRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 同一对象对只插入一次。原事务已撤销时才回填 txn_id，数量保持首次值。
 */
class GenealogyLinkApplicationServiceTest {

    private GenealogyLinkRepository repository;
    private MesTxnReversalRepository reversals;
    private GenealogyLinkApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(GenealogyLinkRepository.class);
        reversals = mock(MesTxnReversalRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T06:00:00Z"), ZoneOffset.ofHours(8));
        service = new GenealogyLinkApplicationService(repository, reversals, session, clock);
    }

    @Test
    void firstLinkInsertsQty() {
        service.link("LOT", 8L, "L-1", "WAFER", 9L, "W1", "CREATE", null, new BigDecimal("1"), 10L);

        verify(repository).save(org.mockito.ArgumentMatchers.<GenealogyLink>argThat(row ->
                row.getStepId().equals(0L) && row.getQty().compareTo(BigDecimal.ONE) == 0 && row.getTxnId().equals(10L)));
    }

    @Test
    void repeatDoesNotChangeQtyUntilOriginalTxnIsReversed() {
        GenealogyLink existing = new GenealogyLink();
        existing.setId(4L);
        existing.setTxnId(10L);
        existing.setQty(BigDecimal.ONE);
        when(repository.find("LOT", 8L, "WAFER", 9L, "CREATE", 0L)).thenReturn(Optional.of(existing));
        when(reversals.existsByOrigTxnId(10L)).thenReturn(false);

        service.link("LOT", 8L, "L-1", "WAFER", 9L, "W1", "CREATE", 0L, new BigDecimal("9"), 11L);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).backfillTxnId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(existing.getQty()).isEqualByComparingTo("1");
    }

    @Test
    void reversedLinkPointsAtNewTxn() {
        GenealogyLink existing = new GenealogyLink();
        existing.setId(4L);
        existing.setTxnId(10L);
        when(repository.find("LOT", 8L, "EQUIPMENT", 3L, "PROCESS", 7L)).thenReturn(Optional.of(existing));
        when(reversals.existsByOrigTxnId(10L)).thenReturn(true);

        service.link("LOT", 8L, "L-1", "EQUIPMENT", 3L, "E1", "PROCESS", 7L, null, 12L);

        verify(repository).backfillTxnId(4L, 12L);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unknownLinkTypeRejected() {
        assertThatThrownBy(() -> service.link("LOT", 8L, null, "WAFER", 9L, null, "SHIP", 0L, null, null))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.GENEALOGY_LINK_TYPE_INVALID);
    }
}
