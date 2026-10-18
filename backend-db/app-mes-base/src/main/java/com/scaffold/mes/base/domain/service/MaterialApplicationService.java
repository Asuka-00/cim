package com.scaffold.mes.base.domain.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.MaterialSaveRequest;
import com.scaffold.mes.base.domain.entity.Material;
import com.scaffold.mes.base.domain.repository.MaterialRepository;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * MaterialApplicationService 物料应用服务
 * <p>
 * 编码在租户内唯一。基础计量单位必须存在。被换算引用时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class MaterialApplicationService {

    private static final Set<String> TYPES = Set.of("WAFER", "RAW", "AUX", "CHEMICAL", "GAS", "PACKAGE", "OTHER");

    private final MaterialRepository repository;
    private final UomRepository uomRepository;

    /**
     * 分页查询物料。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param materialCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Material> page(int page, int pageSize, String materialCode) {
        return repository.page(page, pageSize, materialCode);
    }

    /**
     * 新增物料。
     *
     * @param request 物料内容
     * @throws BusinessException 编码已存在，或类型、计量单位不合法
     */
    @Transactional
    public void create(MaterialSaveRequest request) {
        if (repository.findByCode(request.getMaterialCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.MATERIAL_CODE_EXISTS, "物料编码已存在: " + request.getMaterialCode());
        }
        Material material = new Material();
        copy(request, material);
        repository.save(material);
    }

    /**
     * 修改物料。
     *
     * @param id 主键
     * @param request 物料内容
     * @throws BusinessException 物料不存在、编码冲突，或类型、计量单位不合法
     */
    @Transactional
    public void update(Long id, MaterialSaveRequest request) {
        Material material = require(id);
        repository.findByCode(request.getMaterialCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.MATERIAL_CODE_EXISTS,
                            "物料编码已存在: " + request.getMaterialCode());
                });
        copy(request, material);
        repository.update(material);
    }

    /**
     * 删除物料。仍被换算引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 物料不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.MATERIAL_IN_USE, "物料",
                DeleteReferenceUtils.reference("单位换算", used));
        repository.deleteById(id);
    }

    private Material require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.MATERIAL_NOT_FOUND, "物料不存在"));
    }

    private void copy(MaterialSaveRequest request, Material material) {
        String type = request.getMaterialType() == null || request.getMaterialType().isBlank()
                ? "RAW"
                : request.getMaterialType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesBaseErrorCodes.MATERIAL_TYPE_INVALID, "物料类型不合法: " + type);
        }
        if (uomRepository.findById(request.getBaseUomId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.UOM_NOT_FOUND, "物料计量单位不存在");
        }
        material.setMaterialCode(request.getMaterialCode());
        material.setMaterialName(request.getMaterialName());
        material.setMaterialType(type);
        material.setSpec(request.getSpec());
        material.setBaseUomId(request.getBaseUomId());
        material.setIsCritical(request.getIsCritical() == null ? 0 : request.getIsCritical());
        material.setShelfLifeDays(request.getShelfLifeDays());
        material.setNeedIncomingInspect(request.getNeedIncomingInspect() == null ? 0 : request.getNeedIncomingInspect());
        material.setRemark(request.getRemark());
        material.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
