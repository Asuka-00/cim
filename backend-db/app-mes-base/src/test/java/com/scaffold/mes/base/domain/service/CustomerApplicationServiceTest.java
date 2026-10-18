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
import com.scaffold.mes.base.domain.dto.CustomerSaveRequest;
import com.scaffold.mes.base.domain.entity.Customer;
import com.scaffold.mes.base.domain.repository.CustomerRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * 客户编码不能重复，被产品引用时不能删除。
 */
class CustomerApplicationServiceTest {

    private CustomerRepository repository;
    private CustomerApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        service = new CustomerApplicationService(repository);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.findByCode("ACME")).thenReturn(Optional.of(new Customer()));

        assertThatThrownBy(() -> service.create(request()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.CUSTOMER_CODE_EXISTS);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteMissingThrowsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.CUSTOMER_NOT_FOUND);
    }

    @Test
    void deleteRejectsWhenReferenced() {
        Customer customer = new Customer();
        customer.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(repository.countReferences(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.CUSTOMER_IN_USE);
        verify(repository, never()).deleteById(1L);
    }

    @Test
    void deleteSucceedsWhenUnused() {
        Customer customer = new Customer();
        customer.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(customer));
        when(repository.countReferences(1L)).thenReturn(0L);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    private static CustomerSaveRequest request() {
        CustomerSaveRequest request = new CustomerSaveRequest();
        request.setCustomerCode("ACME");
        request.setCustomerName("示例客户");
        request.setContact("张三");
        request.setPhone("13800000000");
        return request;
    }
}
