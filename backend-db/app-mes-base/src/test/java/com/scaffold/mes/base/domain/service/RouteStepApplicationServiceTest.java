package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteStepSaveRequest;
import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 草稿才能改步骤。同一站点可以排两次，序号不能重复。转换站点必须有目标单位。
 */
class RouteStepApplicationServiceTest {

    private RouteStepRepository repository;
    private RouteVersionRepository versionRepository;
    private RouteStepApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(RouteStepRepository.class);
        versionRepository = mock(RouteVersionRepository.class);
        StepRepository stepRepository = mock(StepRepository.class);
        CountUnitRepository countUnitRepository = mock(CountUnitRepository.class);
        EquipmentGroupRepository equipmentGroupRepository = mock(EquipmentGroupRepository.class);
        service = new RouteStepApplicationService(repository, versionRepository, stepRepository, countUnitRepository,
                equipmentGroupRepository);
        when(versionRepository.findById(1L)).thenReturn(Optional.of(version("DRAFT")));
        when(stepRepository.findById(7L)).thenReturn(Optional.of(new Step()));
        when(countUnitRepository.findById(8L)).thenReturn(Optional.of(new CountUnit()));
        when(countUnitRepository.findById(9L)).thenReturn(Optional.of(new CountUnit()));
    }

    @Test
    void createAllowsSameStepOnAnotherSeq() {
        when(repository.findBySeq(1L, 10)).thenReturn(Optional.empty());
        when(repository.findBySeq(1L, 20)).thenReturn(Optional.empty());

        service.create(1L, request(10, 0, null));
        service.create(1L, request(20, 0, null));

        ArgumentCaptor<RouteStep> captor = ArgumentCaptor.forClass(RouteStep.class);
        verify(repository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(RouteStep::getStepId).containsOnly(7L);
        assertThat(captor.getAllValues()).extracting(RouteStep::getSeqNo).containsExactly(10, 20);
    }

    @Test
    void createRejectsDuplicateSeq() {
        when(repository.findBySeq(1L, 10)).thenReturn(Optional.of(new RouteStep()));

        assertThatThrownBy(() -> service.create(1L, request(10, 0, null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_STEP_SEQ_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsConvertWithoutTargetUnit() {
        when(repository.findBySeq(1L, 30)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(1L, request(30, 1, null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_STEP_CONVERT_UNIT_REQUIRED);
    }

    @Test
    void createKeepsConvertTarget() {
        when(repository.findBySeq(1L, 30)).thenReturn(Optional.empty());

        service.create(1L, request(30, 1, 9L));

        ArgumentCaptor<RouteStep> captor = ArgumentCaptor.forClass(RouteStep.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getConvertToUnitId()).isEqualTo(9L);
        assertThat(captor.getValue().getIsUnitConvert()).isEqualTo(1);
    }

    @Test
    void createRejectsNonDraftVersion() {
        when(versionRepository.findById(1L)).thenReturn(Optional.of(version("ACTIVE")));

        assertThatThrownBy(() -> service.create(1L, request(10, 0, null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.ROUTE_VERSION_NOT_DRAFT);
    }

    private static RouteVersion version(String status) {
        RouteVersion version = new RouteVersion();
        version.setId(1L);
        version.setStatus(status);
        return version;
    }

    private static RouteStepSaveRequest request(int seqNo, int convert, Long convertTo) {
        RouteStepSaveRequest request = new RouteStepSaveRequest();
        request.setSeqNo(seqNo);
        request.setStepId(7L);
        request.setIsUnitConvert(convert);
        request.setConvertToUnitId(convertTo);
        return request;
    }
}
