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
import com.scaffold.mes.base.domain.dto.StepEquipmentGroupSaveRequest;
import com.scaffold.mes.base.domain.entity.EquipmentGroup;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.entity.StepEquipmentGroup;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.StepEquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 同一站点不能重复绑定同一个设备组。未传优先级时按 0，数小优先。
 */
class StepEquipmentGroupApplicationServiceTest {

    private StepEquipmentGroupRepository repository;
    private StepEquipmentGroupApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StepEquipmentGroupRepository.class);
        StepRepository stepRepository = mock(StepRepository.class);
        EquipmentGroupRepository equipmentGroupRepository = mock(EquipmentGroupRepository.class);
        service = new StepEquipmentGroupApplicationService(repository, stepRepository, equipmentGroupRepository);
        when(stepRepository.findById(7L)).thenReturn(Optional.of(new Step()));
        when(equipmentGroupRepository.findById(3L)).thenReturn(Optional.of(new EquipmentGroup()));
    }

    @Test
    void createDefaultsPriorityToZero() {
        service.create(request(null));

        ArgumentCaptor<StepEquipmentGroup> captor = ArgumentCaptor.forClass(StepEquipmentGroup.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isZero();
        assertThat(captor.getValue().getStepId()).isEqualTo(7L);
    }

    @Test
    void createKeepsSmallerPriority() {
        service.create(request(1));

        ArgumentCaptor<StepEquipmentGroup> captor = ArgumentCaptor.forClass(StepEquipmentGroup.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo(1);
    }

    @Test
    void createRejectsDuplicate() {
        when(repository.findExact(7L, 3L)).thenReturn(Optional.of(new StepEquipmentGroup()));

        assertThatThrownBy(() -> service.create(request(0)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_EQUIPMENT_GROUP_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsMissingGroup() {
        EquipmentGroupRepository groups = mock(EquipmentGroupRepository.class);
        StepRepository steps = mock(StepRepository.class);
        when(steps.findById(7L)).thenReturn(Optional.of(new Step()));
        when(groups.findById(3L)).thenReturn(Optional.empty());
        StepEquipmentGroupApplicationService isolated = new StepEquipmentGroupApplicationService(repository, steps, groups);

        assertThatThrownBy(() -> isolated.create(request(0)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_GROUP_NOT_FOUND);
    }

    private static StepEquipmentGroupSaveRequest request(Integer priority) {
        StepEquipmentGroupSaveRequest request = new StepEquipmentGroupSaveRequest();
        request.setStepId(7L);
        request.setGroupId(3L);
        request.setPriority(priority);
        return request;
    }
}
