package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.share.integration.mes.base.provider.EquipmentGroupPort;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentGroupPortImpl 设备所属设备组
 * <p>
 * 从设备台账读取 groupId。没有设备时为空。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class EquipmentGroupPortImpl implements EquipmentGroupPort {

    private final EquipmentRepository equipment;

    @Override
    public Long groupId(Long equipmentId) {
        if (equipmentId == null) {
            return null;
        }
        return equipment.findById(equipmentId).map(row -> row.getGroupId()).orElse(null);
    }
}
