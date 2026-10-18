package com.scaffold.mes.exec.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.core.domain.repository.alert.MesAlertRepository;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.HandoverConfirmRequest;
import com.scaffold.mes.exec.domain.dto.HandoverCreateRequest;
import com.scaffold.mes.exec.domain.entity.Handover;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.repository.HandoverRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.EquipmentAbnormalPort;

/**
 * 新建交接班时状态是草稿。单号重复时拒绝。提交时写入在制数量。已提交的可以由接班人确认。
 */
class HandoverApplicationServiceTest {

    private HandoverRepository repository;
    private LotRepository lots;
    private EquipmentAbnormalPort abnormalEquipment;
    private MesAlertRepository alerts;
    private HandoverApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(HandoverRepository.class);
        lots = mock(LotRepository.class);
        abnormalEquipment = mock(EquipmentAbnormalPort.class);
        alerts = mock(MesAlertRepository.class);
        when(repository.findByNo("HO-1")).thenReturn(Optional.empty());
        when(lots.listInProcess()).thenReturn(List.of());
        when(abnormalEquipment.count()).thenReturn(0L);
        when(alerts.countOpen()).thenReturn(0L);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"), ZoneOffset.ofHours(8));
        service = new HandoverApplicationService(repository, lots, abnormalEquipment, alerts, clock);
    }

    @Test
    void createsDraft() {
        HandoverCreateRequest request = new HandoverCreateRequest();
        request.setHandoverNo("HO-1");
        request.setFromUser("lee");
        request.setRemark("夜班");

        service.create(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<Handover>argThat(row ->
                "DRAFT".equals(row.getStatus()) && "lee".equals(row.getFromUser()) && "夜班".equals(row.getRemark())));
    }

    @Test
    void duplicateNoRejected() {
        when(repository.findByNo("HO-1")).thenReturn(Optional.of(new Handover()));
        HandoverCreateRequest request = new HandoverCreateRequest();
        request.setHandoverNo("HO-1");
        request.setFromUser("lee");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.HANDOVER_INVALID);
    }

    @Test
    void submitMovesDraftToSubmitted() {
        Handover handover = new Handover();
        handover.setStatus("DRAFT");
        when(repository.findById(7L)).thenReturn(Optional.of(handover));

        service.submit(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<Handover>argThat(row ->
                "SUBMITTED".equals(row.getStatus())
                        && LocalDateTime.parse("2026-10-16T16:00").equals(row.getSubmittedAt())
                        && "{\"wipLotCount\":0,\"wipQty\":0,\"wipHoldCount\":0,\"holdLotNos\":[],\"abnormalEquipmentCount\":0,\"openAlertCount\":0}".equals(row.getContentJson())));
    }

    @Test
    void submitWritesWipCounts() {
        Handover handover = new Handover();
        handover.setStatus("DRAFT");
        when(repository.findById(7L)).thenReturn(Optional.of(handover));
        Lot first = new Lot();
        first.setQty(10L);
        first.setHoldCount(1);
        first.setLotNo("L-1");
        Lot second = new Lot();
        second.setQty(5L);
        second.setLotNo("L-2");
        when(lots.listInProcess()).thenReturn(List.of(first, second));
        when(abnormalEquipment.count()).thenReturn(3L);
        when(alerts.countOpen()).thenReturn(4L);

        service.submit(7L);

        verify(repository).update(org.mockito.ArgumentMatchers.<Handover>argThat(row ->
                "{\"wipLotCount\":2,\"wipQty\":15,\"wipHoldCount\":1,\"holdLotNos\":[\"L-1\"],\"abnormalEquipmentCount\":3,\"openAlertCount\":4}".equals(row.getContentJson())));
    }

    @Test
    void submitRejectsSubmitted() {
        Handover handover = new Handover();
        handover.setStatus("SUBMITTED");
        when(repository.findById(7L)).thenReturn(Optional.of(handover));

        assertThatThrownBy(() -> service.submit(7L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.HANDOVER_STATUS);
    }

    @Test
    void confirmMovesSubmittedToConfirmed() {
        Handover handover = new Handover();
        handover.setStatus("SUBMITTED");
        when(repository.findById(7L)).thenReturn(Optional.of(handover));
        HandoverConfirmRequest request = new HandoverConfirmRequest();
        request.setToUser("chen");

        service.confirm(7L, request);

        verify(repository).update(org.mockito.ArgumentMatchers.<Handover>argThat(row ->
                "CONFIRMED".equals(row.getStatus())
                        && "chen".equals(row.getToUser())
                        && LocalDateTime.parse("2026-10-16T16:00").equals(row.getConfirmedAt())));
    }

    @Test
    void confirmRejectsDraft() {
        Handover handover = new Handover();
        handover.setStatus("DRAFT");
        when(repository.findById(7L)).thenReturn(Optional.of(handover));
        HandoverConfirmRequest request = new HandoverConfirmRequest();
        request.setToUser("chen");

        assertThatThrownBy(() -> service.confirm(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.HANDOVER_STATUS);
    }

    @Test
    void confirmRejectsBlankToUser() {
        HandoverConfirmRequest request = new HandoverConfirmRequest();
        request.setToUser(" ");

        assertThatThrownBy(() -> service.confirm(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesExecErrorCodes.HANDOVER_STATUS);
    }
}
