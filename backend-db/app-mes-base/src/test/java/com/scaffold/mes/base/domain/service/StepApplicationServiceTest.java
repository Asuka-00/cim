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
import com.scaffold.mes.base.domain.dto.StepSaveRequest;
import com.scaffold.mes.base.domain.entity.Area;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.entity.StepGroup;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.domain.repository.StepGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 加工站点默认要设备和 Recipe。目检、HOLD、SHIP、仓储默认都不要。
 */
class StepApplicationServiceTest {

    private StepRepository repository;
    private AreaRepository areaRepository;
    private StepGroupRepository stepGroupRepository;
    private StepApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StepRepository.class);
        areaRepository = mock(AreaRepository.class);
        stepGroupRepository = mock(StepGroupRepository.class);
        service = new StepApplicationService(repository, areaRepository, stepGroupRepository);
    }

    @Test
    void createProcessDefaultsToNeedingResources() {
        service.create(request(null));

        Step saved = captured();
        assertThat(saved.getStepType()).isEqualTo("PROCESS");
        assertThat(saved.getNeedEquipment()).isEqualTo(1);
        assertThat(saved.getNeedRecipe()).isEqualTo(1);
    }

    @Test
    void createHoldDefaultsToNoResources() {
        service.create(request("HOLD"));

        Step saved = captured();
        assertThat(saved.getNeedEquipment()).isZero();
        assertThat(saved.getNeedRecipe()).isZero();
    }

    @Test
    void createKeepsExplicitResourceFlags() {
        StepSaveRequest request = request("HOLD");
        request.setNeedEquipment(1);

        service.create(request);

        assertThat(captured().getNeedEquipment()).isEqualTo(1);
        assertThat(captured().getNeedRecipe()).isZero();
    }

    @Test
    void createRejectsUnknownType() {
        assertThatThrownBy(() -> service.create(request("BAKE")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_TYPE_INVALID);
        verify(repository, never()).save(any());
    }

    @Test
    void createKeepsExistingStepGroup() {
        StepSaveRequest request = request("PROCESS");
        request.setStepGroupId(6L);
        when(stepGroupRepository.findById(6L)).thenReturn(Optional.of(new StepGroup()));

        service.create(request);

        assertThat(captured().getStepGroupId()).isEqualTo(6L);
    }

    @Test
    void createRejectsMissingStepGroup() {
        StepSaveRequest request = request("PROCESS");
        request.setStepGroupId(6L);
        when(stepGroupRepository.findById(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_GROUP_NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsMissingArea() {
        StepSaveRequest request = request("PROCESS");
        request.setAreaId(4L);
        when(areaRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.AREA_NOT_FOUND);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("S-1")).thenReturn(Optional.of(new Step()));

        assertThatThrownBy(() -> service.create(request("PROCESS")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_CODE_EXISTS);
    }

    @Test
    void createAcceptsExistingArea() {
        StepSaveRequest request = request("INSPECT");
        request.setAreaId(4L);
        when(areaRepository.findById(4L)).thenReturn(Optional.of(new Area()));

        service.create(request);

        assertThat(captured().getAreaId()).isEqualTo(4L);
        assertThat(captured().getNeedEquipment()).isZero();
    }

    private Step captured() {
        ArgumentCaptor<Step> captor = ArgumentCaptor.forClass(Step.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static StepSaveRequest request(String type) {
        StepSaveRequest request = new StepSaveRequest();
        request.setStepCode("S-1");
        request.setStepName("示例站点");
        request.setStepType(type);
        return request;
    }
}
