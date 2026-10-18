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
import com.scaffold.mes.base.domain.dto.BomSaveRequest;
import com.scaffold.mes.base.domain.entity.Bom;
import com.scaffold.mes.base.domain.repository.BomRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * BOM 编码不能重复，已有版本时不能删除。
 */
class BomApplicationServiceTest {

    private BomRepository repository;
    private BomApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(BomRepository.class);
        service = new BomApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("BOM-1")).thenReturn(Optional.of(new Bom()));

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.BOM_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRejectsWhenVersionExists() {
        Bom bom = new Bom();
        bom.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(bom));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.BOM_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        Bom bom = new Bom();
        bom.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(bom));
        when(repository.countReferences(1L)).thenReturn(0L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private static BomSaveRequest request() {
        BomSaveRequest request = new BomSaveRequest();
        request.setBomCode("BOM-1");
        request.setBomName("主材清单");
        return request;
    }
}
