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
import com.scaffold.mes.base.domain.dto.ProductSaveRequest;
import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.domain.repository.CustomerRepository;
import com.scaffold.mes.base.domain.repository.ProductFamilyRepository;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 产品料号不能重复。类型、晶圆尺寸和关联主数据不合法时拒绝保存。被换算引用时不能删除。
 */
class ProductApplicationServiceTest {

    private ProductRepository repository;
    private CountUnitRepository countUnitRepository;
    private ProductFamilyRepository familyRepository;
    private CustomerRepository customerRepository;
    private ProductApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        countUnitRepository = mock(CountUnitRepository.class);
        familyRepository = mock(ProductFamilyRepository.class);
        customerRepository = mock(CustomerRepository.class);
        service = new ProductApplicationService(repository, countUnitRepository, familyRepository, customerRepository);
        when(countUnitRepository.findById(7L)).thenReturn(Optional.of(new CountUnit()));
    }

    @Test
    void createDefaultsTypeToWafer() {
        service.create(request());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getProductType()).isEqualTo("WAFER");
        assertThat(captor.getValue().getBaseUnitId()).isEqualTo(7L);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("P-1")).thenReturn(Optional.of(new Product()));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsUnknownType() {
        ProductSaveRequest request = request();
        request.setProductType("CHIP");

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_TYPE_INVALID);
    }

    @Test
    void createRejectsUnknownWaferSize() {
        ProductSaveRequest request = request();
        request.setWaferSizeMm(450);

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_WAFER_SIZE_INVALID);
    }

    @Test
    void createRejectsMissingBaseUnit() {
        when(countUnitRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.COUNT_UNIT_NOT_FOUND);
    }

    @Test
    void createRejectsMissingFamily() {
        ProductSaveRequest request = request();
        request.setFamilyId(3L);
        when(familyRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_FAMILY_NOT_FOUND);
    }

    @Test
    void deleteRejectsWhenReferenced() {
        Product product = new Product();
        product.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        when(repository.countReferences(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.PRODUCT_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    private static ProductSaveRequest request() {
        ProductSaveRequest request = new ProductSaveRequest();
        request.setProductCode("P-1");
        request.setProductName("示例产品");
        request.setBaseUnitId(7L);
        request.setWaferSizeMm(300);
        return request;
    }
}
