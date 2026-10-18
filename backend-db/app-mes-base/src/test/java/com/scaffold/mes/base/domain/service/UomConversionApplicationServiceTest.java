package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.UomConversionSaveRequest;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.entity.UomConversion;
import com.scaffold.mes.base.domain.repository.UomConversionRepository;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 换算系数必须大于 0。查找先用限定物料，没有再退回通用。
 */
class UomConversionApplicationServiceTest {

    private UomConversionRepository repository;
    private UomRepository uomRepository;
    private UomConversionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(UomConversionRepository.class);
        uomRepository = mock(UomRepository.class);
        service = new UomConversionApplicationService(repository, uomRepository);
        when(uomRepository.findById(1L)).thenReturn(Optional.of(new Uom()));
        when(uomRepository.findById(2L)).thenReturn(Optional.of(new Uom()));
    }

    @Test
    void createRejectsNonPositiveFactor() {
        UomConversionSaveRequest request = request(null);
        request.setFactor(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.UOM_CONVERSION_FACTOR_INVALID);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicate() {
        when(repository.findExact(1L, 2L, 0L)).thenReturn(Optional.of(new UomConversion()));

        assertThatThrownBy(() -> service.create(request(null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.UOM_CONVERSION_EXISTS);
    }

    @Test
    void resolvePrefersMaterialSpecific() {
        UomConversion specific = row(8L, new BigDecimal("2.5"));
        when(repository.findExact(1L, 2L, 9L)).thenReturn(Optional.of(specific));

        assertThat(service.resolve(1L, 2L, 9L).getFactor()).isEqualByComparingTo("2.5");
        verify(repository, never()).findExact(1L, 2L, 0L);
    }

    @Test
    void resolveFallsBackToGeneric() {
        UomConversion generic = row(3L, new BigDecimal("1000"));
        when(repository.findExact(1L, 2L, 9L)).thenReturn(Optional.empty());
        when(repository.findExact(1L, 2L, 0L)).thenReturn(Optional.of(generic));

        assertThat(service.resolve(1L, 2L, 9L).getId()).isEqualTo(3L);
    }

    @Test
    void resolveMissingThrowsNotFound() {
        when(repository.findExact(1L, 2L, 0L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve(1L, 2L, null))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.UOM_CONVERSION_NOT_FOUND);
    }

    private static UomConversionSaveRequest request(Long materialId) {
        UomConversionSaveRequest request = new UomConversionSaveRequest();
        request.setFromUomId(1L);
        request.setToUomId(2L);
        request.setMaterialId(materialId);
        request.setFactor(new BigDecimal("1000"));
        return request;
    }

    private static UomConversion row(Long id, BigDecimal factor) {
        UomConversion conversion = new UomConversion();
        conversion.setId(id);
        conversion.setFactor(factor);
        return conversion;
    }
}
