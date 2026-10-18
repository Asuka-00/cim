package com.scaffold.mes.base.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.ProductUnitConvCheck;
import com.scaffold.mes.base.domain.dto.ProductUnitConvSaveRequest;
import com.scaffold.mes.base.domain.entity.ProductUnitConv;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.ProductUnitConvRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * ProductUnitConvApplicationService 产品计数换算应用服务
 * <p>
 * 同一产品的源单位和目标单位只能有一条。核对时用系数算出参考数量，再比较实际数量的偏差百分比。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class ProductUnitConvApplicationService {

    private static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("5");

    private final ProductUnitConvRepository repository;
    private final ProductRepository productRepository;
    private final CountUnitRepository countUnitRepository;

    /**
     * 分页查询某个产品的换算。
     *
     * @param productId 产品
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public PageDTO<ProductUnitConv> page(Long productId, int page, int pageSize) {
        requireProduct(productId);
        return repository.page(page, pageSize, productId);
    }

    /**
     * 按系数核对实际数量是否落在允许偏差内。
     *
     * @param productId 产品
     * @param fromUnitId 源计数单位
     * @param toUnitId 目标计数单位
     * @param fromQty 源数量
     * @param actualQty 实际目标数量
     * @return 参考数量、偏差百分比和是否在允许范围内
     * @throws BusinessException 换算不存在，或源数量不大于 0
     */
    public ProductUnitConvCheck check(Long productId, Long fromUnitId, Long toUnitId, BigDecimal fromQty,
            BigDecimal actualQty) {
        if (fromQty == null || fromQty.signum() <= 0 || actualQty == null || actualQty.signum() < 0) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_FACTOR_INVALID, "核对数量不合法");
        }
        ProductUnitConv conversion = repository.findExact(productId, fromUnitId, toUnitId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_NOT_FOUND, "产品计数换算不存在"));
        BigDecimal reference = fromQty.multiply(conversion.getFactor());
        BigDecimal deviation = actualQty.subtract(reference).abs()
                .multiply(new BigDecimal("100"))
                .divide(reference, 4, RoundingMode.HALF_UP);
        return new ProductUnitConvCheck(reference, deviation, deviation.compareTo(conversion.getTolerancePct()) <= 0);
    }

    /**
     * 新增换算。
     *
     * @param productId 产品
     * @param request 换算内容
     * @throws BusinessException 产品或单位不存在、系数或偏差无效、组合已存在
     */
    @Transactional
    public void create(Long productId, ProductUnitConvSaveRequest request) {
        requireProduct(productId);
        requireUnits(request);
        requireFactor(request.getFactor());
        BigDecimal tolerance = toleranceOf(request);
        if (repository.findExact(productId, request.getFromUnitId(), request.getToUnitId()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_EXISTS, "产品计数换算已存在");
        }
        ProductUnitConv conversion = new ProductUnitConv();
        copy(productId, request, conversion, tolerance);
        repository.save(conversion);
    }

    /**
     * 修改换算。
     *
     * @param productId 产品
     * @param id 主键
     * @param request 换算内容
     * @throws BusinessException 记录不存在、组合冲突，或系数、偏差、单位不合法
     */
    @Transactional
    public void update(Long productId, Long id, ProductUnitConvSaveRequest request) {
        ProductUnitConv conversion = require(productId, id);
        requireUnits(request);
        requireFactor(request.getFactor());
        BigDecimal tolerance = toleranceOf(request);
        repository.findExact(productId, request.getFromUnitId(), request.getToUnitId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_EXISTS, "产品计数换算已存在");
                });
        copy(productId, request, conversion, tolerance);
        repository.update(conversion);
    }

    /**
     * 删除换算。
     *
     * @param productId 产品
     * @param id 主键
     * @throws BusinessException 记录不存在或不属于该产品
     */
    @Transactional
    public void delete(Long productId, Long id) {
        require(productId, id);
        repository.deleteById(id);
    }

    private ProductUnitConv require(Long productId, Long id) {
        ProductUnitConv conversion = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_NOT_FOUND, "产品计数换算不存在"));
        if (!productId.equals(conversion.getProductId())) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_NOT_FOUND, "产品计数换算不存在");
        }
        return conversion;
    }

    private void requireProduct(Long productId) {
        if (productRepository.findById(productId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在");
        }
    }

    private void requireUnits(ProductUnitConvSaveRequest request) {
        if (countUnitRepository.findById(request.getFromUnitId()).isEmpty()
                || countUnitRepository.findById(request.getToUnitId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.COUNT_UNIT_NOT_FOUND, "计数单位不存在");
        }
    }

    private static void requireFactor(BigDecimal factor) {
        if (factor == null || factor.signum() <= 0) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_FACTOR_INVALID, "换算系数必须大于 0");
        }
    }

    private static BigDecimal toleranceOf(ProductUnitConvSaveRequest request) {
        BigDecimal tolerance = request.getTolerancePct() == null ? DEFAULT_TOLERANCE : request.getTolerancePct();
        if (tolerance.signum() < 0) {
            throw new BusinessException(MesBaseErrorCodes.PRODUCT_UNIT_CONV_TOLERANCE_INVALID, "允许偏差百分比不能为负");
        }
        return tolerance;
    }

    private static void copy(Long productId, ProductUnitConvSaveRequest request, ProductUnitConv conversion,
            BigDecimal tolerance) {
        conversion.setProductId(productId);
        conversion.setFromUnitId(request.getFromUnitId());
        conversion.setToUnitId(request.getToUnitId());
        conversion.setFactor(request.getFactor());
        conversion.setTolerancePct(tolerance);
        conversion.setRemark(request.getRemark());
    }
}
