package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ProductFamilySaveRequest;
import com.scaffold.mes.base.domain.entity.ProductFamily;
import com.scaffold.mes.base.domain.repository.ProductFamilyRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * ProductFamilyApplicationService 产品族应用服务
 * <p>
 * 编码在租户内唯一。被产品引用时只能停用，不能物理删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ProductFamilyApplicationService {

    private final ProductFamilyRepository repository;

    /**
     * 分页查询产品族。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param familyCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<ProductFamily> page(int page, int pageSize, String familyCode) {
        return repository.page(page, pageSize, familyCode);
    }

    /**
     * 新增产品族。
     *
     * @param request 产品族内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(ProductFamilySaveRequest request) {
        if (repository.findByCode(request.getFamilyCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_FAMILY_CODE_EXISTS, "产品族编码已存在: " + request.getFamilyCode());
        }
        ProductFamily family = new ProductFamily();
        copy(request, family);
        repository.save(family);
    }

    /**
     * 修改产品族。
     *
     * @param id 主键
     * @param request 产品族内容
     * @throws BusinessException 产品族不存在，或编码与其他记录冲突
     */
    @Transactional
    public void update(Long id, ProductFamilySaveRequest request) {
        ProductFamily family = require(id);
        repository.findByCode(request.getFamilyCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.PRODUCT_FAMILY_CODE_EXISTS,
                            "产品族编码已存在: " + request.getFamilyCode());
                });
        copy(request, family);
        repository.update(family);
    }

    /**
     * 删除产品族。仍被产品引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 产品族不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.PRODUCT_FAMILY_IN_USE, "产品族",
                DeleteReferenceUtils.reference("产品", used));
        repository.deleteById(id);
    }

    private ProductFamily require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_FAMILY_NOT_FOUND, "产品族不存在"));
    }

    private static void copy(ProductFamilySaveRequest request, ProductFamily family) {
        family.setFamilyCode(request.getFamilyCode());
        family.setFamilyName(request.getFamilyName());
        family.setRemark(request.getRemark());
        family.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
