package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepGroupSaveRequest;
import com.scaffold.mes.base.domain.entity.StepGroup;
import com.scaffold.mes.base.domain.repository.StepGroupRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 站点组编码不能重复，被站点引用时不能删除。
 */
class StepGroupApplicationServiceTest {

    private StepGroupRepository repository;
    private StepGroupApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(StepGroupRepository.class);
        service = new StepGroupApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("PHOTO")).thenReturn(Optional.of(new StepGroup()));

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_GROUP_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRejectsWhenReferenced() {
        StepGroup group = new StepGroup();
        group.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(group));
        when(repository.countReferences(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.STEP_GROUP_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        StepGroup group = new StepGroup();
        group.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(group));
        when(repository.countReferences(1L)).thenReturn(0L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private static StepGroupSaveRequest request() {
        StepGroupSaveRequest request = new StepGroupSaveRequest();
        request.setGroupCode("PHOTO");
        request.setGroupName("光刻");
        return request;
    }
}
