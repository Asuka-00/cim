package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.SamplingCounter;
import com.scaffold.mes.exec.domain.repository.SamplingCounterRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 每进一个 Lot 加 1。满 N 抽中并清零。
 */
class SamplingCounterApplicationServiceTest {

    private SamplingCounterRepository repository;
    private SamplingCounterApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(SamplingCounterRepository.class);
        when(repository.find(4L, 7L, 0L, 0L)).thenReturn(Optional.empty());
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new SamplingCounterApplicationService(repository, clock);
    }

    @Test
    void firstLotOnlyIncrements() {
        assertThat(service.advance(4L, 7L, null, null, 3, 11L)).isFalse();
        verify(repository).save(org.mockito.ArgumentMatchers.<SamplingCounter>argThat(row ->
                row.getCounter().equals(1) && row.getLastSampledLotId() == null));
    }

    @Test
    void thirdLotHitsAndResets() {
        SamplingCounter existing = new SamplingCounter();
        existing.setCounter(2);
        when(repository.find(4L, 7L, 0L, 0L)).thenReturn(Optional.of(existing));

        assertThat(service.advance(4L, 7L, 0L, 0L, 3, 13L)).isTrue();
        assertThat(existing.getCounter()).isZero();
        assertThat(existing.getLastSampledLotId()).isEqualTo(13L);
        assertThat(existing.getLastSampledAt()).isEqualTo(java.time.LocalDateTime.of(2026, 10, 16, 16, 0));
        verify(repository).update(existing);
    }

    @Test
    void intervalBelowOneRejected() {
        assertThatThrownBy(() -> service.advance(4L, 7L, 0L, 0L, 0, 11L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.SAMPLING_INTERVAL);
    }

    @Test
    void resetClearsCounterAndKeepsLastLot() {
        SamplingCounter existing = new SamplingCounter();
        existing.setCounter(2);
        existing.setLastSampledLotId(13L);
        when(repository.find(4L, 7L, 0L, 0L)).thenReturn(Optional.of(existing));

        service.reset(4L, 7L, null, null);

        assertThat(existing.getCounter()).isZero();
        assertThat(existing.getLastSampledLotId()).isEqualTo(13L);
        verify(repository).update(existing);
    }

    @Test
    void resetMissingCounterDoesNothing() {
        service.reset(4L, 7L, 0L, 0L);
        verify(repository, never()).update(org.mockito.ArgumentMatchers.any());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void everyLotIsSampledWhenIntervalIsOne() {
        assertThat(service.advance(4L, 7L, 0L, 0L, 1, 11L)).isTrue();
    }

    @Test
    void halfTheLotsAreSampled() {
        assertThat(service.advanceByPercent(4L, 7L, 0L, 0L, new java.math.BigDecimal("50"), 11L)).isFalse();
        SamplingCounter existing = new SamplingCounter();
        existing.setCounter(1);
        when(repository.find(4L, 7L, 0L, 0L)).thenReturn(Optional.of(existing));

        assertThat(service.advanceByPercent(4L, 7L, 0L, 0L, new java.math.BigDecimal("50"), 12L)).isTrue();
        assertThat(existing.getCounter()).isZero();
    }
}
