package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.CapabilityDecision;
import com.scaffold.mes.base.domain.dto.EquipmentCapabilitySaveRequest;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.mes.base.domain.entity.EquipmentCapability;
import com.scaffold.mes.base.domain.repository.EquipmentCapabilityRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;

/**
 * Inhibit 不能被更泛的有效资格盖过。完全没有记录时才看设备组绑定。
 */
class EquipmentCapabilityApplicationServiceTest {

    private EquipmentCapabilityRepository repository;
    private EquipmentRepository equipmentRepository;
    private StepRepository stepRepository;
    private EquipmentCapabilityApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(EquipmentCapabilityRepository.class);
        equipmentRepository = mock(EquipmentRepository.class);
        stepRepository = mock(StepRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T04:00:00Z"), ZoneOffset.ofHours(8));
        service = new EquipmentCapabilityApplicationService(repository, equipmentRepository, stepRepository, clock);
    }

    @Test
    void listReturnsEquipmentRows() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(new Equipment()));
        EquipmentCapability row = new EquipmentCapability();
        row.setStepId(7L);
        when(repository.listByEquipment(1L)).thenReturn(List.of(row));

        assertThat(service.list(1L)).extracting(EquipmentCapability::getStepId).containsExactly(7L);
    }

    @Test
    void updateSuspendsWithoutCreatingAnotherRow() {
        EquipmentCapability current = new EquipmentCapability();
        current.setId(4L);
        when(repository.findById(4L)).thenReturn(Optional.of(current));
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(new Equipment()));
        when(stepRepository.findById(7L)).thenReturn(Optional.of(new Step()));
        when(repository.findExact(1L, 7L, 0L, 0L)).thenReturn(Optional.of(current));
        EquipmentCapabilitySaveRequest request = new EquipmentCapabilitySaveRequest();
        request.setEquipmentId(1L);
        request.setStepId(7L);
        request.setStatus("SUSPENDED");

        service.update(4L, request);

        assertThat(current.getStatus()).isEqualTo("SUSPENDED");
        verify(repository).update(current);
    }

    @Test
    void deleteMissingThrows() {
        when(repository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(4L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.EQUIPMENT_CAPABILITY_NOT_FOUND);
    }

    @Test
    void inhibitBlocksEvenWhenGenericIsValid() {
        when(repository.listByEquipmentAndStep(1L, 7L)).thenReturn(List.of(
                row(0L, 0L, "VALID", null),
                row(4L, 9L, "SUSPENDED", null)));

        CapabilityDecision decision = service.decide(1L, 7L, 4L, 9L, true);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.code()).isEqualTo("INHIBIT");
    }

    @Test
    void specificValidMatchAllows() {
        when(repository.listByEquipmentAndStep(1L, 7L)).thenReturn(List.of(row(4L, 0L, "VALID", null)));

        assertThat(service.decide(1L, 7L, 4L, 9L, false).code()).isEqualTo("MATCH");
    }

    @Test
    void outsideWindowDenies() {
        EquipmentCapability row = row(0L, 0L, "VALID", LocalDateTime.of(2026, 10, 16, 11, 0));
        when(repository.listByEquipmentAndStep(1L, 7L)).thenReturn(List.of(row));

        assertThat(service.decide(1L, 7L, 4L, 9L, true).code()).isEqualTo("OUT_OF_WINDOW");
    }

    @Test
    void noRecordUsesGroupBinding() {
        when(repository.listByEquipmentAndStep(1L, 7L)).thenReturn(List.of());

        assertThat(service.decide(1L, 7L, 4L, 9L, true).code()).isEqualTo("GROUP");
        assertThat(service.decide(1L, 7L, 4L, 9L, false).code()).isEqualTo("NO_CAPABILITY");
    }

    @Test
    void expireDueMarksValidRows() {
        EquipmentCapability due = row(0L, 0L, "VALID", LocalDateTime.of(2026, 10, 16, 11, 0));
        when(repository.listDue(LocalDateTime.of(2026, 10, 16, 12, 0))).thenReturn(List.of(due));

        assertThat(service.expireDue()).isEqualTo(1);
        assertThat(due.getStatus()).isEqualTo("EXPIRED");
    }

    private static EquipmentCapability row(long recipeId, long productId, String status, LocalDateTime validTo) {
        EquipmentCapability capability = new EquipmentCapability();
        capability.setEquipmentId(1L);
        capability.setStepId(7L);
        capability.setRecipeId(recipeId);
        capability.setProductId(productId);
        capability.setStatus(status);
        capability.setValidTo(validTo);
        return capability;
    }
}
