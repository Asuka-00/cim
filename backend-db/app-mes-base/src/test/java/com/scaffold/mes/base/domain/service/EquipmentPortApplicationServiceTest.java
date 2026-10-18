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
import com.scaffold.mes.base.domain.dto.EquipmentPortSaveRequest;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.mes.base.domain.repository.EquipmentPortRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 新端口固定为 STANDBY。改台账不会改掉当前状态。上限必须大于 0。
 */
class EquipmentPortApplicationServiceTest {

    private EquipmentPortRepository repository;
    private EquipmentStatusApplicationService statusService;
    private EquipmentPortApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentPortRepository.class);
        EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
        statusService = mock(EquipmentStatusApplicationService.class);
        service = new EquipmentPortApplicationService(repository, equipmentRepository, statusService);
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(new Equipment()));
    }

    @Test
    void createStartsInStandby() {
        service.create(1L, request("C1", "CHAMBER", 2));

        ArgumentCaptor<EquipmentPort> captor = ArgumentCaptor.forClass(EquipmentPort.class);
        verify(repository).save(captor.capture());
        EquipmentPort saved = captor.getValue();
        assertThat(saved.getKind()).isEqualTo("CHAMBER");
        assertThat(saved.getMaxConcurrent()).isEqualTo(2);
        assertThat(saved.getE10State()).isEqualTo("STANDBY");
        assertThat(saved.getStatusSource()).isEqualTo("MANUAL");
        assertThat(saved.getStatusSince()).isNotNull();
        verify(statusService).openInitialChamber(saved);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode(1L, "C1")).thenReturn(Optional.of(new EquipmentPort()));

        assertThatThrownBy(() -> service.create(1L, request("C1", "PORT", null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_PORT_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownKind() {
        assertThatThrownBy(() -> service.create(1L, request("C1", "LOADPORT", 1)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_PORT_KIND_INVALID);
    }

    @Test
    void createRejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> service.create(1L, request("C1", "PORT", 0)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_PORT_CAPACITY_INVALID);
    }

    @Test
    void updateKeepsCurrentState() {
        EquipmentPort current = new EquipmentPort();
        current.setId(4L);
        current.setEquipmentId(1L);
        current.setE10State("PRODUCTIVE");
        current.setStatusSource("TRACK");
        current.setVersion(1);
        when(repository.findById(4L)).thenReturn(Optional.of(current));

        service.update(1L, 4L, request("C1", "CHAMBER", 3));

        ArgumentCaptor<EquipmentPort> captor = ArgumentCaptor.forClass(EquipmentPort.class);
        verify(repository).update(captor.capture());
        assertThat(captor.getValue().getE10State()).isEqualTo("PRODUCTIVE");
        assertThat(captor.getValue().getStatusSource()).isEqualTo("TRACK");
        assertThat(captor.getValue().getMaxConcurrent()).isEqualTo(3);
    }

    private static EquipmentPortSaveRequest request(String code, String kind, Integer capacity) {
        EquipmentPortSaveRequest request = new EquipmentPortSaveRequest();
        request.setPortCode(code);
        request.setKind(kind);
        request.setMaxConcurrent(capacity);
        return request;
    }
}
