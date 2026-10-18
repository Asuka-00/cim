package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.CustomerSaveRequest;
import com.scaffold.mes.base.domain.entity.Customer;
import com.scaffold.mes.base.domain.repository.CustomerRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * CustomerApplicationService 客户应用服务
 * <p>
 * 编码在租户内唯一。被产品引用时只能停用，不能物理删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class CustomerApplicationService {

    private final CustomerRepository repository;

    /**
     * 分页查询客户。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param customerCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Customer> page(int page, int pageSize, String customerCode) {
        return repository.page(page, pageSize, customerCode);
    }

    /**
     * 新增客户。
     *
     * @param request 客户内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(CustomerSaveRequest request) {
        if (repository.findByCode(request.getCustomerCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.CUSTOMER_CODE_EXISTS, "客户编码已存在: " + request.getCustomerCode());
        }
        Customer customer = new Customer();
        copy(request, customer);
        repository.save(customer);
    }

    /**
     * 修改客户。
     *
     * @param id 主键
     * @param request 客户内容
     * @throws BusinessException 客户不存在，或编码与其他记录冲突
     */
    @Transactional
    public void update(Long id, CustomerSaveRequest request) {
        Customer customer = require(id);
        repository.findByCode(request.getCustomerCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.CUSTOMER_CODE_EXISTS, "客户编码已存在: " + request.getCustomerCode());
                });
        copy(request, customer);
        repository.update(customer);
    }

    /**
     * 删除客户。仍被产品引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 客户不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.CUSTOMER_IN_USE, "客户",
                DeleteReferenceUtils.reference("产品", used));
        repository.deleteById(id);
    }

    private Customer require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.CUSTOMER_NOT_FOUND, "客户不存在"));
    }

    private static void copy(CustomerSaveRequest request, Customer customer) {
        customer.setCustomerCode(request.getCustomerCode());
        customer.setCustomerName(request.getCustomerName());
        customer.setContact(request.getContact());
        customer.setPhone(request.getPhone());
        customer.setRemark(request.getRemark());
        customer.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
