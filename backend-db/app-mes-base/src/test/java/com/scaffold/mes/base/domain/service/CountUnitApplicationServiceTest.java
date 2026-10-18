package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.CountUnitSaveRequest;
import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 计数单位编码不能重复，被引用时不能删除。
 */
class CountUnitApplicationServiceTest {

    private CountUnitRepository repository;
    private CountUnitApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CountUnitRepository.class);
        service = new CountUnitApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("WAFER")).thenReturn(Optional.of(new CountUnit()));
        CountUnitSaveRequest request = request();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.COUNT_UNIT_CODE_EXISTS);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRejectsWhenReferenced() {
        CountUnit unit = new CountUnit();
        unit.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(unit));
        when(repository.countReferences(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.COUNT_UNIT_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    private static CountUnitSaveRequest request() {
        CountUnitSaveRequest request = new CountUnitSaveRequest();
        request.setUnitCode("WAFER");
        request.setUnitName("晶圆");
        request.setUnitType("WAFER");
        return request;
    }
}
