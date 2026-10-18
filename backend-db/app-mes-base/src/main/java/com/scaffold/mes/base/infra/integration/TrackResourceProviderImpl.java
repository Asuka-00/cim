package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.entity.EquipmentPort;
import com.scaffold.mes.base.domain.entity.RecipeVersion;
import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.repository.EquipmentPortRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.repository.RecipeVersionRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.StepEquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.mes.base.domain.service.EquipmentCapabilityApplicationService;
import com.scaffold.mes.base.domain.service.RouteStepRecipeApplicationService;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.TrackResourceProvider;
import com.scaffold.share.integration.mes.base.response.TrackResourceFacts;

import lombok.RequiredArgsConstructor;

/**
 * TrackResourceProviderImpl 进站资源查询实现
 * <p>
 * 设备是否可加工走能力判定。Recipe 必须是 ACTIVE，并且出现在步骤覆盖或站点允许列表里。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class TrackResourceProviderImpl implements TrackResourceProvider {

    private final StepRepository stepRepository;
    private final EquipmentRepository equipmentRepository;
    private final EquipmentPortRepository portRepository;
    private final StepEquipmentGroupRepository stepEquipmentGroupRepository;
    private final EquipmentCapabilityApplicationService capabilityService;
    private final RouteStepRepository routeStepRepository;
    private final RouteStepRecipeApplicationService routeStepRecipeService;
    private final RecipeVersionRepository recipeVersionRepository;

    @Override
    public TrackResourceFacts facts(Long stepId, Long routeVersionId, Integer routeSeqNo, Long productId,
            Long equipmentId, Long portId, Long recipeVersionId) {
        Step step = stepRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.STEP_NOT_FOUND, "站点不存在"));
        boolean needEquipment = step.getNeedEquipment() == null || step.getNeedEquipment() != 0;
        boolean needRecipe = step.getNeedRecipe() == null || step.getNeedRecipe() != 0;
        Equipment equipment = equipmentId == null ? null : equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.EQUIPMENT_NOT_FOUND, "设备不存在"));
        Integer portCapacity = portCapacity(equipmentId, portId);
        boolean allowed = equipment != null && equipmentAllowed(stepId, equipment, productId, recipeVersionId);
        if (portId != null && portCapacity == null) {
            allowed = false;
        }
        int capacity = equipment == null || equipment.getMaxConcurrentLots() == null ? 1 : equipment.getMaxConcurrentLots();
        return new TrackResourceFacts(needEquipment, needRecipe, allowed,
                equipment == null ? null : equipment.getE10State(),
                equipment == null ? null : equipment.getStatusSource(),
                capacity, portCapacity, recipeAllowed(stepId, routeVersionId, routeSeqNo, productId, recipeVersionId));
    }

    private Integer portCapacity(Long equipmentId, Long portId) {
        if (portId == null) {
            return null;
        }
        EquipmentPort port = portRepository.findById(portId).orElse(null);
        if (port == null || !port.getEquipmentId().equals(equipmentId)) {
            return null;
        }
        return port.getMaxConcurrent() == null ? 1 : port.getMaxConcurrent();
    }

    private boolean equipmentAllowed(Long stepId, Equipment equipment, Long productId, Long recipeVersionId) {
        boolean groupBound = stepEquipmentGroupRepository.listByStep(stepId).stream()
                .anyMatch(row -> row.getGroupId().equals(equipment.getGroupId()));
        if (stepEquipmentGroupRepository.listByStep(stepId).isEmpty()) {
            groupBound = true;
        }
        Long recipeId = recipeVersionId == null ? null
                : recipeVersionRepository.findById(recipeVersionId).map(RecipeVersion::getRecipeId).orElse(null);
        return capabilityService.decide(equipment.getId(), stepId, recipeId, productId, groupBound).allowed();
    }

    private boolean recipeAllowed(Long stepId, Long routeVersionId, Integer routeSeqNo, Long productId, Long recipeVersionId) {
        if (recipeVersionId == null) {
            return false;
        }
        RecipeVersion version = recipeVersionRepository.findById(recipeVersionId).orElse(null);
        if (version == null || !"ACTIVE".equals(version.getStatus())) {
            return false;
        }
        RouteStep routeStep = routeStepRepository.listByVersion(routeVersionId).stream()
                .filter(row -> stepId.equals(row.getStepId()) && routeSeqNo.equals(row.getSeqNo()))
                .findFirst()
                .orElse(null);
        if (routeStep == null) {
            return false;
        }
        return routeStepRecipeService.resolve(routeStep.getId(), productId).stream()
                .anyMatch(row -> version.getRecipeId().equals(row.getRecipeId()));
    }
}
