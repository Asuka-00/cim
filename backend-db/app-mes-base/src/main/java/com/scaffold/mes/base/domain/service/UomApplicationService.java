package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.UomSaveRequest;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * UomApplicationService 物料计量单位应用服务
 * <p>
 * 编码在租户内唯一。被换算或物料引用时只能停用，不能物理删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class UomApplicationService {

    private final UomRepository repository;

    /**
     * 分页查询物料计量单位。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param uomCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Uom> page(int page, int pageSize, String uomCode) {
        return repository.page(page, pageSize, uomCode);
    }

    /**
     * 新增物料计量单位。
     *
     * @param request 单位内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(UomSaveRequest request) {
        if (repository.findByCode(request.getUomCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.UOM_CODE_EXISTS, "物料计量单位编码已存在: " + request.getUomCode());
        }
        Uom uom = new Uom();
        copy(request, uom);
        repository.save(uom);
    }

    /**
     * 修改物料计量单位。
     *
     * @param id 主键
     * @param request 单位内容
     * @throws BusinessException 单位不存在，或编码与其他记录冲突
     */
    @Transactional
    public void update(Long id, UomSaveRequest request) {
        Uom uom = require(id);
        repository.findByCode(request.getUomCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.UOM_CODE_EXISTS, "物料计量单位编码已存在: " + request.getUomCode());
                });
        copy(request, uom);
        repository.update(uom);
    }

    /**
     * 删除物料计量单位。仍被引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 单位不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.UOM_IN_USE, "物料计量单位",
                DeleteReferenceUtils.reference("换算或物料", used));
        repository.deleteById(id);
    }

    private Uom require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.UOM_NOT_FOUND, "物料计量单位不存在"));
    }

    private static void copy(UomSaveRequest request, Uom uom) {
        uom.setUomCode(request.getUomCode());
        uom.setUomName(request.getUomName());
        uom.setDimension(request.getDimension());
        uom.setDecimals(request.getDecimals() == null ? 0 : request.getDecimals());
        uom.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
