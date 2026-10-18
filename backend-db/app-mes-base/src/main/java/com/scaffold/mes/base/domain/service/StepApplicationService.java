package com.scaffold.mes.base.domain.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepSaveRequest;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.domain.repository.StepGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * StepApplicationService 站点应用服务
 * <p>
 * 编码在租户内唯一。目检、HOLD、SHIP、仓储类站点在未显式指定时，不要求设备和 Recipe。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepApplicationService {

    private static final Set<String> TYPES = Set.of(
            "PROCESS", "MEASURE", "INSPECT", "HOLD", "SHIP", "CONVERT", "PACK", "STORE");
    private static final Set<String> NO_RESOURCE_TYPES = Set.of("INSPECT", "HOLD", "SHIP", "STORE");

    private final StepRepository repository;
    private final AreaRepository areaRepository;
    private final StepGroupRepository stepGroupRepository;

    /**
     * 分页查询站点。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param stepCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Step> page(int page, int pageSize, String stepCode) {
        return repository.page(page, pageSize, stepCode);
    }

    /**
     * 新增站点。
     *
     * @param request 站点内容
     * @throws BusinessException 编码已存在，或类型、区域不合法
     */
    @Transactional
    public void create(StepSaveRequest request) {
        if (repository.findByCode(request.getStepCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_CODE_EXISTS, "站点编码已存在: " + request.getStepCode());
        }
        Step step = new Step();
        copy(request, step);
        repository.save(step);
    }

    /**
     * 修改站点。
     *
     * @param id 主键
     * @param request 站点内容
     * @throws BusinessException 站点不存在、编码冲突，或类型、区域不合法
     */
    @Transactional
    public void update(Long id, StepSaveRequest request) {
        Step step = require(id);
        repository.findByCode(request.getStepCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.STEP_CODE_EXISTS, "站点编码已存在: " + request.getStepCode());
                });
        copy(request, step);
        repository.update(step);
    }

    /**
     * 删除站点。仍被引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 站点不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.STEP_IN_USE, "站点",
                DeleteReferenceUtils.reference("工艺数据", used));
        repository.deleteById(id);
    }

    private Step require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在"));
    }

    private void copy(StepSaveRequest request, Step step) {
        String type = request.getStepType() == null || request.getStepType().isBlank() ? "PROCESS" : request.getStepType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesBaseErrorCodes.STEP_TYPE_INVALID, "站点类型不合法: " + type);
        }
        if (request.getAreaId() != null && areaRepository.findById(request.getAreaId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.AREA_NOT_FOUND, "区域不存在");
        }
        if (request.getStepGroupId() != null && stepGroupRepository.findById(request.getStepGroupId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_GROUP_NOT_FOUND, "站点组不存在");
        }
        int resourceDefault = NO_RESOURCE_TYPES.contains(type) ? 0 : 1;
        step.setStepCode(request.getStepCode());
        step.setStepName(request.getStepName());
        step.setStepType(type);
        step.setAreaId(request.getAreaId());
        step.setStepGroupId(request.getStepGroupId());
        step.setNeedEquipment(request.getNeedEquipment() == null ? resourceDefault : request.getNeedEquipment());
        step.setNeedRecipe(request.getNeedRecipe() == null ? resourceDefault : request.getNeedRecipe());
        step.setDescription(request.getDescription());
        step.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
