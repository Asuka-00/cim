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
import org.mockito.ArgumentCaptor;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ProductUnitConvSaveRequest;
import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.entity.ProductUnitConv;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.ProductUnitConvRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 1 Wafer = 1200 Die。实际数量相对参考数量的偏差超过允许百分比时，核对结果为超出。
 */
class ProductUnitConvApplicationServiceTest {

    private ProductUnitConvRepository repository;
    private ProductUnitConvApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductUnitConvRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        CountUnitRepository countUnitRepository = mock(CountUnitRepository.class);
        service = new ProductUnitConvApplicationService(repository, productRepository, countUnitRepository);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));
        when(countUnitRepository.findById(10L)).thenReturn(Optional.of(new CountUnit()));
        when(countUnitRepository.findById(11L)).thenReturn(Optional.of(new CountUnit()));
    }

    @Test
    void createDefaultsToleranceToFive() {
        service.create(1L, request(new BigDecimal("1200"), null));

        ArgumentCaptor<ProductUnitConv> captor = ArgumentCaptor.forClass(ProductUnitConv.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTolerancePct()).isEqualByComparingTo("5");
        assertThat(captor.getValue().getProductId()).isEqualTo(1L);
    }

    @Test
    void createRejectsNonPositiveFactor() {
        assertThatThrownBy(() -> service.create(1L, request(BigDecimal.ZERO, null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_UNIT_CONV_FACTOR_INVALID);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsDuplicate() {
        when(repository.findExact(1L, 10L, 11L)).thenReturn(Optional.of(new ProductUnitConv()));

        assertThatThrownBy(() -> service.create(1L, request(new BigDecimal("1200"), null)))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_UNIT_CONV_EXISTS);
    }

    @Test
    void checkAcceptsQuantityInsideTolerance() {
        when(repository.findExact(1L, 10L, 11L)).thenReturn(Optional.of(row()));

        var result = service.check(1L, 10L, 11L, BigDecimal.ONE, new BigDecimal("1230"));

        assertThat(result.referenceQty()).isEqualByComparingTo("1200");
        assertThat(result.deviationPct()).isEqualByComparingTo("2.5000");
        assertThat(result.withinTolerance()).isTrue();
    }

    @Test
    void checkFlagsQuantityOutsideTolerance() {
        when(repository.findExact(1L, 10L, 11L)).thenReturn(Optional.of(row()));

        var result = service.check(1L, 10L, 11L, BigDecimal.ONE, new BigDecimal("1000"));

        assertThat(result.withinTolerance()).isFalse();
    }

    @Test
    void checkMissingThrowsNotFound() {
        when(repository.findExact(1L, 10L, 11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.check(1L, 10L, 11L, BigDecimal.ONE, new BigDecimal("1200")))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_UNIT_CONV_NOT_FOUND);
    }

    private static ProductUnitConvSaveRequest request(BigDecimal factor, BigDecimal tolerance) {
        ProductUnitConvSaveRequest request = new ProductUnitConvSaveRequest();
        request.setFromUnitId(10L);
        request.setToUnitId(11L);
        request.setFactor(factor);
        request.setTolerancePct(tolerance);
        return request;
    }

    private static ProductUnitConv row() {
        ProductUnitConv conversion = new ProductUnitConv();
        conversion.setId(5L);
        conversion.setProductId(1L);
        conversion.setFactor(new BigDecimal("1200"));
        conversion.setTolerancePct(new BigDecimal("5"));
        return conversion;
    }
}
