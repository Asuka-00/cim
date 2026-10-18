package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.share.integration.mes.base.provider.EquipmentAbnormalPort;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentAbnormalPortImpl 非计划停机设备计数
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class EquipmentAbnormalPortImpl implements EquipmentAbnormalPort {

    private final EquipmentRepository equipmentRepository;

    @Override
    public long count() {
        return equipmentRepository.countByE10State("UNSCHEDULED_DOWN");
    }
}
