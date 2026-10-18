package com.scaffold.mes.base.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.StepEquipmentGroupSaveRequest;
import com.scaffold.mes.base.domain.entity.StepEquipmentGroup;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.StepEquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * StepEquipmentGroupApplicationService 站点设备组绑定应用服务
 * <p>
 * 同一站点不能重复绑定同一个设备组。priority 越小越优先。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class StepEquipmentGroupApplicationService {

    private final StepEquipmentGroupRepository repository;
    private final StepRepository stepRepository;
    private final EquipmentGroupRepository equipmentGroupRepository;

    /**
     * 列出某个站点的设备组绑定。
     *
     * @param stepId 站点
     * @return 按优先级从小到大的绑定
     * @throws BusinessException 站点不存在
     */
    public List<StepEquipmentGroup> list(Long stepId) {
        requireStep(stepId);
        return repository.listByStep(stepId);
    }

    /**
     * 新增绑定。
     *
     * @param request 绑定内容
     * @throws BusinessException 站点或设备组不存在，或已经绑定
     */
    @Transactional
    public void create(StepEquipmentGroupSaveRequest request) {
        requireStep(request.getStepId());
        requireGroup(request.getGroupId());
        if (repository.findExact(request.getStepId(), request.getGroupId()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_EQUIPMENT_GROUP_EXISTS, "站点已绑定该设备组");
        }
        StepEquipmentGroup binding = new StepEquipmentGroup();
        copy(request, binding);
        repository.save(binding);
    }

    /**
     * 修改绑定。可以改优先级，也可以换设备组。
     *
     * @param id 主键
     * @param request 绑定内容
     * @throws BusinessException 绑定不存在，或换组后与其他记录冲突
     */
    @Transactional
    public void update(Long id, StepEquipmentGroupSaveRequest request) {
        StepEquipmentGroup binding = require(id);
        requireStep(request.getStepId());
        requireGroup(request.getGroupId());
        repository.findExact(request.getStepId(), request.getGroupId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.STEP_EQUIPMENT_GROUP_EXISTS, "站点已绑定该设备组");
                });
        copy(request, binding);
        repository.update(binding);
    }

    /**
     * 删除绑定。
     *
     * @param id 主键
     * @throws BusinessException 绑定不存在
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        repository.deleteById(id);
    }

    private void copy(StepEquipmentGroupSaveRequest request, StepEquipmentGroup binding) {
        binding.setStepId(request.getStepId());
        binding.setGroupId(request.getGroupId());
        binding.setPriority(request.getPriority() == null ? 0 : request.getPriority());
    }

    private void requireStep(Long stepId) {
        if (stepRepository.findById(stepId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在");
        }
    }

    private void requireGroup(Long groupId) {
        if (equipmentGroupRepository.findById(groupId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_GROUP_NOT_FOUND, "设备组不存在");
        }
    }

    private StepEquipmentGroup require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.STEP_EQUIPMENT_GROUP_NOT_FOUND, "站点设备组绑定不存在"));
    }
}
