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
import com.scaffold.mes.base.domain.dto.ProductFamilySaveRequest;
import com.scaffold.mes.base.domain.entity.ProductFamily;
import com.scaffold.mes.base.domain.repository.ProductFamilyRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 产品族编码不能重复，被产品引用时不能删除。
 */
class ProductFamilyApplicationServiceTest {

    private ProductFamilyRepository repository;
    private ProductFamilyApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductFamilyRepository.class);
        service = new ProductFamilyApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("LOGIC")).thenReturn(Optional.of(new ProductFamily()));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_FAMILY_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsCodeUsedByAnother() {
        ProductFamily current = new ProductFamily();
        current.setId(1L);
        ProductFamily other = new ProductFamily();
        other.setId(2L);
        when(repository.findById(1L)).thenReturn(Optional.of(current));
        when(repository.findByCode("LOGIC")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1L, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_FAMILY_CODE_EXISTS);
        verify(repository, never()).update(any());
    }

    @Test
    void deleteRejectsWhenReferenced() {
        ProductFamily family = new ProductFamily();
        family.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(family));
        when(repository.countReferences(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_FAMILY_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        ProductFamily family = new ProductFamily();
        family.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(family));
        when(repository.countReferences(1L)).thenReturn(0L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private static ProductFamilySaveRequest request() {
        ProductFamilySaveRequest request = new ProductFamilySaveRequest();
        request.setFamilyCode("LOGIC");
        request.setFamilyName("逻辑芯片");
        return request;
    }
}
