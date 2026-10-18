package com.scaffold.mes.base.domain.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ProductSaveRequest;
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.domain.repository.CustomerRepository;
import com.scaffold.mes.base.domain.repository.ProductFamilyRepository;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * ProductApplicationService 产品应用服务
 * <p>
 * 料号在租户内唯一。基础计数单位必须存在；产品族和客户有值时也必须存在。被换算引用时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ProductApplicationService {

    private static final Set<String> TYPES = Set.of("WAFER", "DIE", "PACKAGE", "OTHER");
    private static final Set<Integer> WAFER_SIZES = Set.of(150, 200, 300);

    private final ProductRepository repository;
    private final CountUnitRepository countUnitRepository;
    private final ProductFamilyRepository familyRepository;
    private final CustomerRepository customerRepository;

    /**
     * 分页查询产品。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param productCode 料号模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Product> page(int page, int pageSize, String productCode) {
        return repository.page(page, pageSize, productCode);
    }

    /**
     * 新增产品。
     *
     * @param request 产品内容
     * @throws BusinessException 料号已存在，或类型、尺寸、关联主数据不合法
     */
    @Transactional
    public void create(ProductSaveRequest request) {
        if (repository.findByCode(request.getProductCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_CODE_EXISTS, "产品料号已存在: " + request.getProductCode());
        }
        Product product = new Product();
        copy(request, product);
        repository.save(product);
    }

    /**
     * 修改产品。
     *
     * @param id 主键
     * @param request 产品内容
     * @throws BusinessException 产品不存在，料号冲突，或关联主数据不合法
     */
    @Transactional
    public void update(Long id, ProductSaveRequest request) {
        Product product = require(id);
        repository.findByCode(request.getProductCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.PRODUCT_CODE_EXISTS, "产品料号已存在: " + request.getProductCode());
                });
        copy(request, product);
        repository.update(product);
    }

    /**
     * 删除产品。仍被计数换算引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 产品不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.PRODUCT_IN_USE, "产品",
                DeleteReferenceUtils.reference("计数换算", used));
        repository.deleteById(id);
    }

    private Product require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在"));
    }

    private void copy(ProductSaveRequest request, Product product) {
        String type = request.getProductType() == null || request.getProductType().isBlank()
                ? "WAFER"
                : request.getProductType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_TYPE_INVALID, "产品类型不合法: " + type);
        }
        if (request.getWaferSizeMm() != null && !WAFER_SIZES.contains(request.getWaferSizeMm())) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_WAFER_SIZE_INVALID, "晶圆尺寸不合法: " + request.getWaferSizeMm());
        }
        if (countUnitRepository.findById(request.getBaseUnitId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.COUNT_UNIT_NOT_FOUND, "计数单位不存在");
        }
        if (request.getFamilyId() != null && familyRepository.findById(request.getFamilyId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_FAMILY_NOT_FOUND, "产品族不存在");
        }
        if (request.getCustomerId() != null && customerRepository.findById(request.getCustomerId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.CUSTOMER_NOT_FOUND, "客户不存在");
        }
        product.setProductCode(request.getProductCode());
        product.setProductName(request.getProductName());
        product.setProductType(type);
        product.setFamilyId(request.getFamilyId());
        product.setCustomerId(request.getCustomerId());
        product.setCustomerPartNo(request.getCustomerPartNo());
        product.setWaferSizeMm(request.getWaferSizeMm());
        product.setGrossDiePerWafer(request.getGrossDiePerWafer());
        product.setBaseUnitId(request.getBaseUnitId());
        product.setRemark(request.getRemark());
        product.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
