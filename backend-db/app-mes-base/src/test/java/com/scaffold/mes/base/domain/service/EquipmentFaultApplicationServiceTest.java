package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EquipmentFaultCloseRequest;
import com.scaffold.mes.base.domain.dto.EquipmentFaultOpenRequest;
import com.scaffold.mes.base.domain.dto.EquipmentStatusChangeRequest;
import com.scaffold.mes.base.domain.entity.EquipmentFault;
import com.scaffold.mes.base.domain.entity.EquipmentFaultLot;
import com.scaffold.mes.base.domain.repository.EquipmentFaultRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;

/**
 * 登记异常时记下受影响的 Lot。关闭后写下处理结果，不能再次关闭。
 */
class EquipmentFaultApplicationServiceTest {

    private EquipmentFaultRepository repository;
    private EquipmentStatusApplicationService equipmentStatus;
    private EquipmentFault fault;
    private EquipmentFaultApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentFaultRepository.class);
        CurrentSessionProvider session = mock(CurrentSessionProvider.class);
        when(session.getCurrentUserNameOrDefault()).thenReturn("sam");
        when(repository.findByNo("F-1")).thenReturn(Optional.empty());
        fault = new EquipmentFault();
        fault.setId(7L);
        fault.setStatus("OPEN");
        when(repository.findById(7L)).thenReturn(Optional.of(fault));
        equipmentStatus = mock(EquipmentStatusApplicationService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T22:00:00Z"), ZoneOffset.ofHours(8));
        service = new EquipmentFaultApplicationService(repository, equipmentStatus, session, clock);
    }

    @Test
    void openCountsAffectedLots() {
        EquipmentFaultOpenRequest request = new EquipmentFaultOpenRequest();
        request.setFaultNo("F-1");
        request.setEquipmentId(3L);
        request.setDescription("真空报警");
        request.setLotIds(List.of(8L, 9L));

        service.open(request);

        verify(repository).save(org.mockito.ArgumentMatchers.<EquipmentFault>argThat(row ->
                "OPEN".equals(row.getStatus()) && row.getAffectedLotCount().equals(2) && "sam".equals(row.getReportedBy())));
        verify(repository).saveLot(org.mockito.ArgumentMatchers.<EquipmentFaultLot>argThat(row -> row.getLotId().equals(8L)));
        verify(equipmentStatus, never()).change(any(), any());
    }

    @Test
    void checkedDownSetsUnscheduledDown() {
        EquipmentFaultOpenRequest request = new EquipmentFaultOpenRequest();
        request.setFaultNo("F-1");
        request.setEquipmentId(3L);
        request.setDescription("真空报警");
        request.setDownEquipment(true);

        service.open(request);

        verify(equipmentStatus).change(eq(3L), org.mockito.ArgumentMatchers.<EquipmentStatusChangeRequest>argThat(row ->
                "UNSCHEDULED_DOWN".equals(row.getE10State()) && "FAULT".equals(row.getReasonCode())));
    }

    @Test
    void closeWritesSolution() {
        EquipmentFaultCloseRequest request = new EquipmentFaultCloseRequest();
        request.setSolution("更换密封圈");

        service.close(7L, request);

        assertThat(fault.getStatus()).isEqualTo("CLOSED");
        assertThat(fault.getSolution()).isEqualTo("更换密封圈");
    }

    @Test
    void closedCannotCloseAgain() {
        fault.setStatus("CLOSED");
        EquipmentFaultCloseRequest request = new EquipmentFaultCloseRequest();
        request.setSolution("再次处理");

        assertThatThrownBy(() -> service.close(7L, request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_FAULT_CLOSED);
    }
}
