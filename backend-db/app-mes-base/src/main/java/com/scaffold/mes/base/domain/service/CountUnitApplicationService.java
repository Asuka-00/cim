package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.CountUnitSaveRequest;
import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * CountUnitApplicationService 计数单位应用服务
 * <p>
 * 编码在租户内唯一。被产品或换算引用时只能停用，不能物理删除。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class CountUnitApplicationService {

    private final CountUnitRepository repository;

    /**
     * 分页查询计数单位。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param unitCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<CountUnit> page(int page, int pageSize, String unitCode) {
        return repository.page(page, pageSize, unitCode);
    }

    /**
     * 新增计数单位。
     *
     * @param request 单位内容
     * @throws BusinessException 编码已存在
     */
    @Transactional
    public void create(CountUnitSaveRequest request) {
        if (repository.findByCode(request.getUnitCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.COUNT_UNIT_CODE_EXISTS, "计数单位编码已存在: " + request.getUnitCode());
        }
        CountUnit unit = new CountUnit();
        copy(request, unit);
        repository.save(unit);
    }

    /**
     * 修改计数单位。
     *
     * @param id 主键
     * @param request 单位内容
     * @throws BusinessException 单位不存在，或编码与其他记录冲突
     */
    @Transactional
    public void update(Long id, CountUnitSaveRequest request) {
        CountUnit unit = require(id);
        repository.findByCode(request.getUnitCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.COUNT_UNIT_CODE_EXISTS, "计数单位编码已存在: " + request.getUnitCode());
                });
        copy(request, unit);
        repository.update(unit);
    }

    /**
     * 删除计数单位。仍被引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 单位不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.COUNT_UNIT_IN_USE, "计数单位",
                DeleteReferenceUtils.reference("产品或换算", used));
        repository.deleteById(id);
    }

    private CountUnit require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.COUNT_UNIT_NOT_FOUND, "计数单位不存在"));
    }

    private static void copy(CountUnitSaveRequest request, CountUnit unit) {
        unit.setUnitCode(request.getUnitCode());
        unit.setUnitName(request.getUnitName());
        unit.setUnitType(request.getUnitType());
        unit.setIsBase(request.getIsBase() == null ? 0 : request.getIsBase());
        unit.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        unit.setRemark(request.getRemark());
        unit.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
