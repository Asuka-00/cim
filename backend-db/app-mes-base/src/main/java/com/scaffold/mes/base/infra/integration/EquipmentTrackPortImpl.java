package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.service.EquipmentStatusApplicationService;
import com.scaffold.share.integration.mes.base.provider.EquipmentTrackPort;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentTrackPortImpl 进站设备状态联动
 * <p>
 * 把设备改成加工态，历史段的来源是 TRACK。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class EquipmentTrackPortImpl implements EquipmentTrackPort {

    private final EquipmentStatusApplicationService statusService;
    private final EquipmentRepository equipmentRepository;

    @Override
    public void onTrackIn(Long equipmentId, String e10State) {
        statusService.applyTrack(equipmentId, e10State);
    }

    @Override
    public void releaseIfIdle(Long equipmentId, long otherRunningLots) {
        if (equipmentId == null || otherRunningLots > 0) {
            return;
        }
        Equipment equipment = equipmentRepository.findById(equipmentId).orElse(null);
        if (equipment != null && "TRACK".equals(equipment.getStatusSource())) {
            statusService.applyTrack(equipmentId, "STANDBY");
        }
    }
}
