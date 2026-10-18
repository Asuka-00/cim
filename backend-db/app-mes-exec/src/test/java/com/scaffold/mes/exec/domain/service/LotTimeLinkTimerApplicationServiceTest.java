package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotTimeLinkTimer;
import com.scaffold.mes.exec.domain.repository.LotTimeLinkTimerRepository;
import com.scaffold.share.integration.mes.base.provider.TimeLinkProvider;
import com.scaffold.share.integration.mes.base.response.TimeLinkStart;

/**
 * 起点出站按最长分钟数开 ACTIVE 计时。没有约束时不写。
 */
class LotTimeLinkTimerApplicationServiceTest {

    @Test
    void openSetsDeadline() {
        TimeLinkProvider links = mock(TimeLinkProvider.class);
        LotTimeLinkTimerRepository repository = mock(LotTimeLinkTimerRepository.class);
        when(links.startingAt(21L, 10)).thenReturn(List.of(new TimeLinkStart(3L, 10, 30, 30, "HOLD", "QT")));
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        LocalDateTime start = LocalDateTime.of(2026, 10, 16, 8, 0);
        LotTimeLinkTimerApplicationService service = new LotTimeLinkTimerApplicationService(links, repository,
                mock(LotHoldApplicationService.class));

        service.open(lot, start);

        verify(repository).save(org.mockito.ArgumentMatchers.<LotTimeLinkTimer>argThat(row -> {
            assertThat(row.getStatus()).isEqualTo("ACTIVE");
            assertThat(row.getDeadlineAt()).isEqualTo(start.plusMinutes(30));
            assertThat(row.getToSeqNo()).isEqualTo(30);
            return true;
        }));
    }

    @Test
    void noLinkDoesNotSave() {
        TimeLinkProvider links = mock(TimeLinkProvider.class);
        LotTimeLinkTimerRepository repository = mock(LotTimeLinkTimerRepository.class);
        when(links.startingAt(21L, 10)).thenReturn(List.of());
        Lot lot = new Lot();
        lot.setRouteVersionId(21L);
        lot.setRouteSeqNo(10);
        new LotTimeLinkTimerApplicationService(links, repository, mock(LotHoldApplicationService.class))
                .open(lot, LocalDateTime.of(2026, 10, 16, 8, 0));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closeMarksMatchingTimerDone() {
        LotTimeLinkTimerRepository repository = mock(LotTimeLinkTimerRepository.class);
        LotTimeLinkTimer arrived = timer(30);
        LotTimeLinkTimer other = timer(40);
        when(repository.listActive(8L)).thenReturn(List.of(arrived, other));
        Lot lot = new Lot();
        lot.setId(8L);
        lot.setRouteSeqNo(30);
        new LotTimeLinkTimerApplicationService(mock(TimeLinkProvider.class), repository, mock(LotHoldApplicationService.class))
                .close(lot);

        assertThat(arrived.getStatus()).isEqualTo("DONE");
        assertThat(other.getStatus()).isEqualTo("ACTIVE");
        verify(repository).update(arrived);
        verify(repository, never()).update(other);
    }

    @Test
    void scanHoldsOverdueTimer() {
        LotTimeLinkTimerRepository repository = mock(LotTimeLinkTimerRepository.class);
        LotHoldApplicationService holds = mock(LotHoldApplicationService.class);
        LotTimeLinkTimer timer = timer(30);
        timer.setId(9L);
        timer.setLotId(8L);
        timer.setAction("HOLD");
        timer.setReasonCode("QT");
        LocalDateTime now = LocalDateTime.of(2026, 10, 16, 9, 0);
        when(repository.listOverdue(now)).thenReturn(List.of(timer));
        LotTimeLinkTimerApplicationService service = new LotTimeLinkTimerApplicationService(mock(TimeLinkProvider.class),
                repository, holds);

        assertThat(service.scan(now)).isEqualTo(1);
        verify(holds).holdSystem(8L, "TIME_LINK", "QT", 9L);
        assertThat(timer.getAlertLevel()).isEqualTo("BREACHED");
    }

    private static LotTimeLinkTimer timer(int toSeq) {
        LotTimeLinkTimer timer = new LotTimeLinkTimer();
        timer.setToSeqNo(toSeq);
        timer.setStatus("ACTIVE");
        return timer;
    }
}
