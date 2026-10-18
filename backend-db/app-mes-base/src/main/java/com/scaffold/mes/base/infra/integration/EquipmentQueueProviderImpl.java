package com.scaffold.mes.base.infra.integration;

import java.util.List;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.repository.StepEquipmentGroupRepository;
import com.scaffold.share.integration.mes.base.provider.EquipmentQueueProvider;

import lombok.RequiredArgsConstructor;

/**
 * EquipmentQueueProviderImpl 设备能加工的站点
 * <p>
 * 用设备所属设备组在站点上的绑定。没有设备组时返回空列表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class EquipmentQueueProviderImpl implements EquipmentQueueProvider {

    private final EquipmentRepository equipmentRepository;
    private final StepEquipmentGroupRepository stepEquipmentGroupRepository;

    @Override
    public List<Long> stepIds(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .map(equipment -> equipment.getGroupId())
                .map(groupId -> stepEquipmentGroupRepository.listByGroup(groupId).stream()
                        .map(binding -> binding.getStepId())
                        .distinct()
                        .toList())
                .orElse(List.of());
    }
}
