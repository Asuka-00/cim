package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.UomSaveRequest;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 物料计量单位编码不能重复，被引用时不能删除。
 */
class UomApplicationServiceTest {

    private UomRepository repository;
    private UomApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UomRepository.class);
        service = new UomApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("KG")).thenReturn(Optional.of(new Uom()));
        UomSaveRequest request = request();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.UOM_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteMissingThrowsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.UOM_NOT_FOUND);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        Uom uom = new Uom();
        uom.setId(2L);
        when(repository.findById(2L)).thenReturn(Optional.of(uom));
        when(repository.countReferences(2L)).thenReturn(0L);

        service.delete(2L);

        verify(repository).deleteById(2L);
    }

    @Test
    void deleteRejectsWhenReferenced() {
        Uom uom = new Uom();
        uom.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(uom));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.UOM_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    private static UomSaveRequest request() {
        UomSaveRequest request = new UomSaveRequest();
        request.setUomCode("KG");
        request.setUomName("千克");
        request.setDimension("WEIGHT");
        return request;
    }
}
