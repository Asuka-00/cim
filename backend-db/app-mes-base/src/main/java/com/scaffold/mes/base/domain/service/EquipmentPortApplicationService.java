package com.scaffold.mes.base.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EquipmentPortSaveRequest;
import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.mes.base.domain.repository.EquipmentPortRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentPortApplicationService 设备端口应用服务
 * <p>
 * 编码在设备内唯一。新建时当前状态固定为 STANDBY。修改台账不会改当前状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EquipmentPortApplicationService {

    private static final Set<String> KINDS = Set.of("PORT", "CHAMBER");

    private final EquipmentPortRepository repository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentStatusApplicationService statusService;

    /**
     * 列出某设备的端口。
     *
     * @param equipmentId 设备
     * @return 端口列表
     * @throws BusinessException 设备不存在
     */
    public List<EquipmentPort> list(Long equipmentId) {
        requireEquipment(equipmentId);
        return repository.listByEquipment(equipmentId);
    }

    /**
     * 新增端口。
     *
     * @param equipmentId 设备
     * @param request 端口内容
     * @throws BusinessException 设备不存在，编码重复，或类型、上限不合法
     */
    @Transactional
    public void create(Long equipmentId, EquipmentPortSaveRequest request) {
        requireEquipment(equipmentId);
        if (repository.findByCode(equipmentId, request.getPortCode()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_CODE_EXISTS, "端口编码已存在: " + request.getPortCode());
        }
        EquipmentPort port = new EquipmentPort();
        port.setEquipmentId(equipmentId);
        copyMaster(request, port);
        port.setE10State("STANDBY");
        port.setStatusSource("MANUAL");
        port.setStatusSince(LocalDateTime.now());
        port.setVersion(0);
        repository.save(port);
        statusService.openInitialChamber(port);
    }

    /**
     * 修改端口台账。当前 E10 保持原值。
     *
     * @param equipmentId 设备
     * @param id 端口主键
     * @param request 端口内容
     * @throws BusinessException 端口不存在，或编码与其他端口冲突
     */
    @Transactional
    public void update(Long equipmentId, Long id, EquipmentPortSaveRequest request) {
        EquipmentPort port = require(equipmentId, id);
        repository.findByCode(equipmentId, request.getPortCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_CODE_EXISTS,
                            "端口编码已存在: " + request.getPortCode());
                });
        copyMaster(request, port);
        repository.update(port);
    }

    /**
     * 删除端口。
     *
     * @param equipmentId 设备
     * @param id 端口主键
     * @throws BusinessException 端口不存在
     */
    @Transactional
    public void delete(Long equipmentId, Long id) {
        require(equipmentId, id);
        repository.deleteById(id);
    }

    private void copyMaster(EquipmentPortSaveRequest request, EquipmentPort port) {
        String kind = request.getKind() == null || request.getKind().isBlank() ? "PORT" : request.getKind();
        if (!KINDS.contains(kind)) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_KIND_INVALID, "端口类型不合法: " + kind);
        }
        int capacity = request.getMaxConcurrent() == null ? 1 : request.getMaxConcurrent();
        if (capacity <= 0) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_CAPACITY_INVALID, "同时加工上限必须大于 0");
        }
        port.setPortCode(request.getPortCode());
        port.setPortName(request.getPortName());
        port.setKind(kind);
        port.setMaxConcurrent(capacity);
        port.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
    }

    private void requireEquipment(Long equipmentId) {
        if (equipmentRepository.findById(equipmentId).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_NOT_FOUND, "设备不存在");
        }
    }

    private EquipmentPort require(Long equipmentId, Long id) {
        EquipmentPort port = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_NOT_FOUND, "设备端口不存在"));
        if (!equipmentId.equals(port.getEquipmentId())) {
            throw new BusinessException(MesBaseErrorCodes.EQUIPMENT_PORT_NOT_FOUND, "设备端口不存在");
        }
        return port;
    }
}
