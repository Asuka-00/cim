package com.scaffold.mes.base.domain.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EquipmentSaveRequest;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.utils.DeleteReferenceUtils;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentApplicationService 设备台账应用服务
 * <p>
 * 编码在租户内唯一。新建时当前状态固定为 STANDBY / MANUAL。修改台账不会改当前状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EquipmentApplicationService {

    private static final Set<String> TYPES = Set.of("PROCESS", "MEASURE", "TEST", "PACK", "TRANSPORT", "OTHER");

    private final EquipmentRepository repository;
    private final EquipmentGroupRepository groupRepository;
    private final AreaRepository areaRepository;
    private final EquipmentStatusApplicationService statusService;

    /**
     * 分页查询设备。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param equipmentCode 编码模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<Equipment> page(int page, int pageSize, String equipmentCode) {
        return repository.page(page, pageSize, equipmentCode);
    }

    /**
     * 新增设备。当前状态固定为待机，来源为人工。
     *
     * @param request 台账内容
     * @throws BusinessException 编码已存在，或类型、设备组、区域不合法
     */
    @Transactional
    public void create(EquipmentSaveRequest request) {
        if (repository.findByCode(request.getEquipmentCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_CODE_EXISTS, "设备编码已存在: " + request.getEquipmentCode());
        }
        Equipment equipment = new Equipment();
        copyMaster(request, equipment);
        equipment.setE10State("STANDBY");
        equipment.setStatusSource("MANUAL");
        equipment.setStatusSince(LocalDateTime.now());
        equipment.setVersion(0);
        repository.save(equipment);
        statusService.openInitial(equipment);
    }

    /**
     * 修改台账。当前 E10、原因码和状态来源保持原值。
     *
     * @param id 主键
     * @param request 台账内容
     * @throws BusinessException 设备不存在、编码冲突，或关联主数据不合法
     */
    @Transactional
    public void update(Long id, EquipmentSaveRequest request) {
        Equipment equipment = require(id);
        repository.findByCode(request.getEquipmentCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_CODE_EXISTS,
                            "设备编码已存在: " + request.getEquipmentCode());
                });
        copyMaster(request, equipment);
        repository.update(equipment);
    }

    /**
     * 删除设备。已经切换过状态时拒绝，并先清掉新建时那一段未切换的历史。
     *
     * @param id 主键
     * @throws BusinessException 设备不存在或仍被引用
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        long used = repository.countReferences(id);
        DeleteReferenceUtils.requireNoReferences(MesBaseErrorCodes.EQUIPMENT_IN_USE, "设备",
                DeleteReferenceUtils.reference("状态历史", used));
        statusService.discardUntouchedInitial(id);
        repository.deleteById(id);
    }

    private Equipment require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_NOT_FOUND, "设备不存在"));
    }

    private void copyMaster(EquipmentSaveRequest request, Equipment equipment) {
        String type = request.getEquipmentType() == null || request.getEquipmentType().isBlank()
                ? "PROCESS"
                : request.getEquipmentType();
        if (!TYPES.contains(type)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_TYPE_INVALID, "设备类型不合法: " + type);
        }
        if (request.getGroupId() != null && groupRepository.findById(request.getGroupId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_GROUP_NOT_FOUND, "设备组不存在");
        }
        if (request.getAreaId() != null && areaRepository.findById(request.getAreaId()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.AREA_NOT_FOUND, "区域不存在");
        }
        equipment.setEquipmentCode(request.getEquipmentCode());
        equipment.setEquipmentName(request.getEquipmentName());
        equipment.setEquipmentType(type);
        equipment.setGroupId(request.getGroupId());
        equipment.setAreaId(request.getAreaId());
        equipment.setVendor(request.getVendor());
        equipment.setModel(request.getModel());
        equipment.setSerialNo(request.getSerialNo());
        equipment.setRemark(request.getRemark());
        equipment.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }
}
