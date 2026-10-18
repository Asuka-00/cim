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
import com.scaffold.mes.base.domain.dto.EquipmentGroupSaveRequest;
import com.scaffold.mes.base.domain.entity.Area;
import com.scaffold.mes.base.domain.entity.EquipmentGroup;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 设备组编码不能重复。区域必须存在。被设备引用时不能删除。
 */
class EquipmentGroupApplicationServiceTest {

    private EquipmentGroupRepository repository;
    private AreaRepository areaRepository;
    private EquipmentGroupApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentGroupRepository.class);
        areaRepository = mock(AreaRepository.class);
        service = new EquipmentGroupApplicationService(repository, areaRepository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("LITHO")).thenReturn(Optional.of(new EquipmentGroup()));

        assertThatThrownBy(() -> service.create(request(null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_GROUP_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsMissingArea() {
        when(areaRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request(4L)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.AREA_NOT_FOUND);
    }

    @Test
    void createKeepsExistingArea() {
        when(areaRepository.findById(4L)).thenReturn(Optional.of(new Area()));

        service.create(request(4L));

        ArgumentCaptor<EquipmentGroup> captor = ArgumentCaptor.forClass(EquipmentGroup.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAreaId()).isEqualTo(4L);
        assertThat(captor.getValue().getEnabled()).isEqualTo(1);
    }

    @Test
    void deleteRejectsWhenReferenced() {
        EquipmentGroup group = new EquipmentGroup();
        group.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(group));
        when(repository.countReferences(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_GROUP_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        EquipmentGroup group = new EquipmentGroup();
        group.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(group));
        when(repository.countReferences(1L)).thenReturn(0L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private static EquipmentGroupSaveRequest request(Long areaId) {
        EquipmentGroupSaveRequest request = new EquipmentGroupSaveRequest();
        request.setGroupCode("LITHO");
        request.setGroupName("光刻组");
        request.setAreaId(areaId);
        return request;
    }
}
