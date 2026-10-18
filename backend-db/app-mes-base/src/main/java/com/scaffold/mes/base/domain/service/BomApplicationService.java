package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.BomSaveRequest;
import com.scaffold.mes.base.domain.entity.Bom;
import com.scaffold.mes.base.domain.repository.BomRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * BomApplicationService BOM 应用服务
 * <p>
 * 编码在租户内唯一。已有版本时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class BomApplicationService {

    private final BomRepository repository;

    /**
     * 分页查询 BOM。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param bomCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Bom> page(int page, int pageSize, String bomCode) {
        return repository.page(page, pageSize, bomCode);
    }

    /**
     * 新增 BOM。
     *
     * @param request BOM 内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(BomSaveRequest request) {
        if (repository.findByCode(request.getBomCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.BOM_CODE_EXISTS, "BOM 编码已存在: " + request.getBomCode());
        }
        Bom bom = new Bom();
        copy(request, bom);
        repository.save(bom);
    }

    /**
     * 修改 BOM。
     *
     * @param id 主键
     * @param request BOM 内容
     * @throws BusinessException BOM 不存在，或编码与其他记录冲突
     */
    @Transactional
    public void update(Long id, BomSaveRequest request) {
        Bom bom = require(id);
        repository.findByCode(request.getBomCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.BOM_CODE_EXISTS, "BOM 编码已存在: " + request.getBomCode());
                });
        copy(request, bom);
        repository.update(bom);
    }

    /**
     * 删除 BOM。仍有版本时拒绝。
     *
     * @param id 主键
     * @throws BusinessException BOM 不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.BOM_IN_USE, "BOM",
                DeleteReferenceUtils.reference("版本", used));
        repository.deleteById(id);
    }

    private Bom require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.BOM_NOT_FOUND, "BOM 不存在"));
    }

    private static void copy(BomSaveRequest request, Bom bom) {
        bom.setBomCode(request.getBomCode());
        bom.setBomName(request.getBomName());
        bom.setDescription(request.getDescription());
        bom.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
