package com.scaffold.mes.base.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EquipmentGroupSaveRequest;
import com.scaffold.mes.base.domain.entity.EquipmentGroup;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentGroupApplicationService 设备组应用服务
 * <p>
 * 编码在租户内唯一。所属区域有值时必须存在。被设备引用时只能停用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EquipmentGroupApplicationService {

    private final EquipmentGroupRepository repository;
    private final AreaRepository areaRepository;

    /**
     * 分页查询设备组。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param groupCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<EquipmentGroup> page(int page, int pageSize, String groupCode) {
        return repository.page(page, pageSize, groupCode);
    }

    /**
     * 新增设备组。
     *
     * @param request 设备组内容
     * @throws BusinessException 编码已存在，或区域不存在
     */
    @Transactional
    public void create(EquipmentGroupSaveRequest request) {
        if (repository.findByCode(request.getGroupCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_GROUP_CODE_EXISTS,
                    "设备组编码已存在: " + request.getGroupCode());
        }
        requireArea(request.getAreaId());
        EquipmentGroup group = new EquipmentGroup();
        copy(request, group);
        repository.save(group);
    }

    /**
     * 修改设备组。
     *
     * @param id 主键
     * @param request 设备组内容
     * @throws BusinessException 设备组不存在、编码冲突，或区域不存在
     */
    @Transactional
    public void update(Long id, EquipmentGroupSaveRequest request) {
        EquipmentGroup group = require(id);
        repository.findByCode(request.getGroupCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_GROUP_CODE_EXISTS,
                            "设备组编码已存在: " + request.getGroupCode());
                });
        requireArea(request.getAreaId());
        copy(request, group);
        repository.update(group);
    }

    /**
     * 删除设备组。仍被设备引用时拒绝。
     *
     * @param id 主键
     * @throws BusinessException 设备组不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.EQUIPMENT_GROUP_IN_USE, "设备组",
                DeleteReferenceUtils.reference("设备", used));
        repository.deleteById(id);
    }

    private EquipmentGroup require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_GROUP_NOT_FOUND, "设备组不存在"));
    }

    private void requireArea(Long areaId) {
        if (areaId != null && areaRepository.findById(areaId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.AREA_NOT_FOUND, "区域不存在");
        }
    }

    private static void copy(EquipmentGroupSaveRequest request, EquipmentGroup group) {
        group.setGroupCode(request.getGroupCode());
        group.setGroupName(request.getGroupName());
        group.setAreaId(request.getAreaId());
        group.setRemark(request.getRemark());
        group.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
