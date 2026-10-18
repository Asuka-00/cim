package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EquipmentStatusChangeRequest;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.mes.base.domain.entity.EquipmentStatusHist;
import com.scaffold.mes.base.domain.repository.EquipmentPortRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.repository.EquipmentStatusHistRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 两段状态首尾相接，时长之和等于总经过秒数。停机没有原因码时拒绝。
 */
class EquipmentStatusApplicationServiceTest {

    private final List<EquipmentStatusHist> saved = new ArrayList<>();
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-16T02:00:00Z"));
    private Equipment equipment;
    private EquipmentPortRepository portRepository;
    private EquipmentStatusHistRepository histRepository;
    private EquipmentStatusApplicationService service;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        equipment.setId(1L);
        equipment.setE10State("STANDBY");
        equipment.setStatusSource("MANUAL");
        equipment.setStatusSince(LocalDateTime.of(2026, 10, 16, 10, 0));
        equipment.setVersion(1);
        EquipmentStatusHist open = openSegment("STANDBY", equipment.getStatusSince());
        saved.add(open);

        EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
        portRepository = mock(EquipmentPortRepository.class);
        histRepository = mock(EquipmentStatusHistRepository.class);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        when(histRepository.listOpen(1L, 0L)).thenAnswer(invocation -> saved.stream()
                .filter(hist -> hist.getEndAt() == null)
                .toList());
        doAnswer(invocation -> {
            saved.add(invocation.getArgument(0));
            return null;
        }).when(histRepository).save(any());

        Clock clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.ofHours(8);
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now.get();
            }
        };
        service = new EquipmentStatusApplicationService(equipmentRepository, portRepository, histRepository, clock);
    }

    @Test
    void changeClosesPreviousSegmentAndKeepsTimelineContinuous() {
        now.set(Instant.parse("2026-10-16T02:01:30Z"));
        service.change(1L, request("PRODUCTIVE", null));
        now.set(Instant.parse("2026-10-16T02:02:30Z"));
        service.change(1L, request("STANDBY", null));

        List<EquipmentStatusHist> closed = saved.stream().filter(hist -> hist.getEndAt() != null).toList();
        EquipmentStatusHist current = saved.stream().filter(hist -> hist.getEndAt() == null).findFirst().orElseThrow();
        assertThat(closed).hasSize(2);
        assertThat(closed.get(0).getEndAt()).isEqualTo(closed.get(1).getStartAt());
        assertThat(closed.get(1).getEndAt()).isEqualTo(current.getStartAt());
        assertThat(closed.get(0).getDurationSec() + closed.get(1).getDurationSec()).isEqualTo(150L);
        assertThat(current.getFromE10State()).isEqualTo("PRODUCTIVE");
        assertThat(current.getToE10State()).isEqualTo("STANDBY");
        assertThat(equipment.getE10State()).isEqualTo("STANDBY");
    }

    @Test
    void changeRejectsDownWithoutReason() {
        assertThatThrownBy(() -> service.change(1L, request("UNSCHEDULED_DOWN", " ")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_REASON_REQUIRED);
        assertThat(saved).hasSize(1);
    }

    @Test
    void changeRejectsUnknownE10() {
        EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
        EquipmentStatusHistRepository histRepository = mock(EquipmentStatusHistRepository.class);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(equipment));
        EquipmentStatusApplicationService isolated = new EquipmentStatusApplicationService(equipmentRepository,
                mock(EquipmentPortRepository.class), histRepository, Clock.systemUTC());

        assertThatThrownBy(() -> isolated.change(1L, request("IDLE", null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_E10_INVALID);
        verify(histRepository, never()).save(any());
    }

    @Test
    void changeChamberWritesHistoryOnThatPort() {
        EquipmentPort chamber = new EquipmentPort();
        chamber.setId(8L);
        chamber.setEquipmentId(1L);
        chamber.setKind("CHAMBER");
        chamber.setE10State("STANDBY");
        chamber.setStatusSource("MANUAL");
        chamber.setStatusSince(LocalDateTime.of(2026, 10, 16, 10, 0));
        when(portRepository.findById(8L)).thenReturn(Optional.of(chamber));
        EquipmentStatusHist open = openSegment("STANDBY", chamber.getStatusSince());
        open.setPortId(8L);
        List<EquipmentStatusHist> chamberRows = new ArrayList<>();
        chamberRows.add(open);
        when(histRepository.listOpen(1L, 8L)).thenAnswer(invocation -> chamberRows.stream()
                .filter(hist -> hist.getEndAt() == null)
                .toList());
        doAnswer(invocation -> {
            chamberRows.add(invocation.getArgument(0));
            return null;
        }).when(histRepository).save(any());
        now.set(Instant.parse("2026-10-16T02:01:00Z"));

        service.changeChamber(1L, 8L, request("PRODUCTIVE", null));

        EquipmentStatusHist current = chamberRows.stream().filter(hist -> hist.getEndAt() == null).findFirst().orElseThrow();
        assertThat(current.getPortId()).isEqualTo(8L);
        assertThat(current.getFromE10State()).isEqualTo("STANDBY");
        assertThat(current.getToE10State()).isEqualTo("PRODUCTIVE");
        assertThat(chamber.getE10State()).isEqualTo("PRODUCTIVE");
        assertThat(equipment.getE10State()).isEqualTo("STANDBY");
    }

    @Test
    void changeChamberRejectsPlainPort() {
        EquipmentPort port = new EquipmentPort();
        port.setId(9L);
        port.setEquipmentId(1L);
        port.setKind("PORT");
        when(portRepository.findById(9L)).thenReturn(Optional.of(port));

        assertThatThrownBy(() -> service.changeChamber(1L, 9L, request("PRODUCTIVE", null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_PORT_KIND_INVALID);
        verify(histRepository, never()).save(any());
    }

    private static EquipmentStatusChangeRequest request(String e10, String reason) {
        EquipmentStatusChangeRequest request = new EquipmentStatusChangeRequest();
        request.setE10State(e10);
        request.setReasonCode(reason);
        return request;
    }

    private static EquipmentStatusHist openSegment(String e10, LocalDateTime start) {
        EquipmentStatusHist hist = new EquipmentStatusHist();
        hist.setEquipmentId(1L);
        hist.setPortId(0L);
        hist.setToE10State(e10);
        hist.setStartAt(start);
        hist.setSource("MANUAL");
        return hist;
    }
}
