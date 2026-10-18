package com.scaffold.mes.base.domain.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.UomConversionSaveRequest;
import com.scaffold.mes.base.domain.entity.UomConversion;
import com.scaffold.mes.base.domain.repository.UomConversionRepository;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * UomConversionApplicationService 物料单位换算应用服务
 * <p>
 * 同一源单位、目标单位、物料只能有一条。查找时先用限定物料，没有再退回通用换算。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class UomConversionApplicationService {

    private static final long GENERIC_MATERIAL = 0L;

    private final UomConversionRepository repository;
    private final UomRepository uomRepository;

    /**
     * 分页查询换算。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param fromUomId 源单位，空表示不限
     * @return 分页结果
     */
    public PageDTO<UomConversion> page(int page, int pageSize, Long fromUomId) {
        return repository.page(page, pageSize, fromUomId);
    }

    /**
     * 按查找顺序解析换算：限定物料优先，否则通用。
     *
     * @param fromUomId 源单位
     * @param toUomId 目标单位
     * @param materialId 限定物料，空或 0 只查通用
     * @return 命中的换算
     * @throws BusinessException 没有可用换算
     */
    public UomConversion resolve(Long fromUomId, Long toUomId, Long materialId) {
        long material = materialId == null ? GENERIC_MATERIAL : materialId;
        if (material != GENERIC_MATERIAL) {
            var specific = repository.findExact(fromUomId, toUomId, material);
            if (specific.isPresent()) {
                return specific.get();
            }
        }
        return repository.findExact(fromUomId, toUomId, GENERIC_MATERIAL)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.UOM_CONVERSION_NOT_FOUND, "物料单位换算不存在"));
    }

    /**
     * 新增换算。
     *
     * @param request 换算内容
     * @throws BusinessException 单位不存在、系数无效或组合已存在
     */
    @Transactional
    public void create(UomConversionSaveRequest request) {
        long material = materialOf(request);
        requireUoms(request);
        requireFactor(request.getFactor());
        if (repository.findExact(request.getFromUomId(), request.getToUomId(), material).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.UOM_CONVERSION_EXISTS, "物料单位换算已存在");
        }
        UomConversion conversion = new UomConversion();
        copy(request, conversion, material);
        repository.save(conversion);
    }

    /**
     * 修改换算。
     *
     * @param id 主键
     * @param request 换算内容
     * @throws BusinessException 记录不存在、单位不存在、系数无效或组合与其他记录冲突
     */
    @Transactional
    public void update(Long id, UomConversionSaveRequest request) {
        UomConversion conversion = require(id);
        long material = materialOf(request);
        requireUoms(request);
        requireFactor(request.getFactor());
        repository.findExact(request.getFromUomId(), request.getToUomId(), material)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.UOM_CONVERSION_EXISTS, "物料单位换算已存在");
                });
        copy(request, conversion, material);
        repository.update(conversion);
    }

    /**
     * 删除换算。
     *
     * @param id 主键
     * @throws BusinessException 记录不存在
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        repository.deleteById(id);
    }

    private UomConversion require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.UOM_CONVERSION_NOT_FOUND, "物料单位换算不存在"));
    }

    private void requireUoms(UomConversionSaveRequest request) {
        if (uomRepository.findById(request.getFromUomId()).isEmpty()
                || uomRepository.findById(request.getToUomId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.UOM_NOT_FOUND, "物料计量单位不存在");
        }
    }

    private static void requireFactor(BigDecimal factor) {
        if (factor == null || factor.signum() <= 0) {
            throw new BusinessException(MesBaseErrorCodes.UOM_CONVERSION_FACTOR_INVALID, "换算系数必须大于 0");
        }
    }

    private static long materialOf(UomConversionSaveRequest request) {
        return request.getMaterialId() == null ? GENERIC_MATERIAL : request.getMaterialId();
    }

    private static void copy(UomConversionSaveRequest request, UomConversion conversion, long material) {
        conversion.setFromUomId(request.getFromUomId());
        conversion.setToUomId(request.getToUomId());
        conversion.setMaterialId(material);
        conversion.setFactor(request.getFactor());
    }
}
