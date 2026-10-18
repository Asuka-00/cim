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
import com.scaffold.mes.base.domain.dto.EquipmentSaveRequest;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.entity.EquipmentGroup;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 新建设备固定为 STANDBY。修改台账不会改掉当前状态。
 */
class EquipmentApplicationServiceTest {

    private EquipmentRepository repository;
    private EquipmentGroupRepository groupRepository;
    private EquipmentStatusApplicationService statusService;
    private EquipmentApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentRepository.class);
        groupRepository = mock(EquipmentGroupRepository.class);
        AreaRepository areaRepository = mock(AreaRepository.class);
        statusService = mock(EquipmentStatusApplicationService.class);
        service = new EquipmentApplicationService(repository, groupRepository, areaRepository, statusService);
    }

    @Test
    void createStartsInStandby() {
        service.create(request());

        Equipment saved = capturedSave();
        assertThat(saved.getEquipmentType()).isEqualTo("PROCESS");
        assertThat(saved.getE10State()).isEqualTo("STANDBY");
        assertThat(saved.getStatusSource()).isEqualTo("MANUAL");
        assertThat(saved.getStatusSince()).isNotNull();
        assertThat(saved.getVersion()).isZero();
        verify(statusService).openInitial(saved);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("EQ-1")).thenReturn(Optional.of(new Equipment()));

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownType() {
        EquipmentSaveRequest request = request();
        request.setEquipmentType("OVEN");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_TYPE_INVALID);
    }

    @Test
    void createRejectsMissingGroup() {
        EquipmentSaveRequest request = request();
        request.setGroupId(3L);
        when(groupRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_GROUP_NOT_FOUND);
    }

    @Test
    void updateKeepsCurrentState() {
        Equipment current = new Equipment();
        current.setId(1L);
        current.setE10State("PRODUCTIVE");
        current.setStatusSource("TRACK");
        current.setVersion(2);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(groupRepository.findById(3L)).thenReturn(Optional.of(new EquipmentGroup()));
        EquipmentSaveRequest request = request();
        request.setGroupId(3L);
        request.setEquipmentName("改名");

        service.update(1L, request);

        ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
        verify(repository).update(captor.capture());
        assertThat(captor.getValue().getE10State()).isEqualTo("PRODUCTIVE");
        assertThat(captor.getValue().getStatusSource()).isEqualTo("TRACK");
        assertThat(captor.getValue().getEquipmentName()).isEqualTo("改名");
    }

    @Test
    void deleteRejectsWhenHistoryExists() {
        Equipment equipment = new Equipment();
        equipment.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(equipment));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_IN_USE);
        verify(repository, never()).deleteById(1L);
        verify(statusService, never()).discardUntouchedInitial(1L);
    }

    @Test
    void deleteRemovesInitialSegmentWhenStatusNeverChanged() {
        Equipment equipment = new Equipment();
        equipment.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(equipment));
        when(repository.countReferences(1L)).thenReturn(0L);

        service.delete(1L);

        verify(statusService).discardUntouchedInitial(1L);
        verify(repository).deleteById(1L);
    }

    private Equipment capturedSave() {
        ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static EquipmentSaveRequest request() {
        EquipmentSaveRequest request = new EquipmentSaveRequest();
        request.setEquipmentCode("EQ-1");
        request.setEquipmentName("光刻机");
        return request;
    }
}
