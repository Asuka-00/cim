package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteTimeLinkSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteTimeLink;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteTimeLinkRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * Time Link 的起点必须早于终点，且两步都已排进草稿版本。HOLD 必须有原因码。
 */
class RouteTimeLinkApplicationServiceTest {

    private RouteTimeLinkRepository repository;
    private RouteStepRepository stepRepository;
    private RouteTimeLinkApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(RouteTimeLinkRepository.class);
        RouteVersionRepository versionRepository = mock(RouteVersionRepository.class);
        stepRepository = mock(RouteStepRepository.class);
        service = new RouteTimeLinkApplicationService(repository, versionRepository, stepRepository);
        RouteVersion version = new RouteVersion();
        version.setId(1L);
        version.setStatus("DRAFT");
        when(versionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(stepRepository.findBySeq(1L, 10)).thenReturn(Optional.of(new RouteStep()));
        when(stepRepository.findBySeq(1L, 30)).thenReturn(Optional.of(new RouteStep()));
    }

    @Test
    void createLinksEarlierStepToLaterStep() {
        service.create(1L, request(10, 30, "HOLD", "QT"));

        ArgumentCaptor<RouteTimeLink> captor = ArgumentCaptor.forClass(RouteTimeLink.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMaxMinutes()).isEqualTo(120);
        assertThat(captor.getValue().getAction()).isEqualTo("HOLD");
        assertThat(captor.getValue().getReasonCode()).isEqualTo("QT");
    }

    @Test
    void createRejectsReversedSeq() {
        assertThatThrownBy(() -> service.create(1L, request(30, 10, "WARN", null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_TIME_LINK_SEQ_INVALID);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsMissingStep() {
        when(stepRepository.findBySeq(1L, 30)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, request(10, 30, "WARN", null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_TIME_LINK_SEQ_INVALID);
    }

    @Test
    void createRejectsHoldWithoutReason() {
        assertThatThrownBy(() -> service.create(1L, request(10, 30, "HOLD", " ")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_TIME_LINK_REASON_REQUIRED);
    }

    @Test
    void createRejectsDuplicate() {
        when(repository.findExact(1L, 10, 30)).thenReturn(Optional.of(new RouteTimeLink()));

        assertThatThrownBy(() -> service.create(1L, request(10, 30, "WARN", null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_TIME_LINK_EXISTS);
    }

    private static RouteTimeLinkSaveRequest request(int from, int to, String action, String reason) {
        RouteTimeLinkSaveRequest request = new RouteTimeLinkSaveRequest();
        request.setFromSeqNo(from);
        request.setToSeqNo(to);
        request.setMaxMinutes(120);
        request.setWarnMinutes(90);
        request.setAction(action);
        request.setReasonCode(reason);
        return request;
    }
}
